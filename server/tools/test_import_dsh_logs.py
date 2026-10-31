import contextlib
import io
import tempfile
import unittest
from pathlib import Path

from import_dsh_logs import daily_rows, emit_sql


class ImportDshLogsTest(unittest.TestCase):
    def test_preserves_uncertain_training_as_note_without_plaintext_sql(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "2026-10-23.md"
            path.write_text(
                "## 生活记录\n"
                "| 时间 | 类别 | 记录 | 备注 |\n"
                "|---|---|---|---|\n"
                "| 08:46–09:37 | 运动 | 卧推 5×10–12，每侧 20 kg | 截图记录；次数是区间 |\n"
                "## 日记\n私人日记不导入\n", encoding="utf-8")
            rows = daily_rows(Path(directory))
            self.assertEqual(1, len(rows))
            self.assertEqual("运动 · 08:46–09:37", rows[0][3])
            self.assertIn("10–12", rows[0][4])
            output = io.StringIO()
            with contextlib.redirect_stdout(output):
                emit_sql(rows, "DemoUser")
            sql = output.getvalue()
            self.assertIn("'NOTE'", sql)
            self.assertIn("ON DUPLICATE KEY UPDATE id = id", sql)
            self.assertNotIn("私人日记", sql)
            self.assertNotIn("卧推", sql)

    def test_rejects_malformed_rows_before_sql_is_emitted(self):
        with tempfile.TemporaryDirectory() as directory:
            (Path(directory) / "2026-10-23.md").write_text(
                "## 生活记录\n| 时间 | 类别 | 记录 | 备注 |\n"
                "| 08:00 | 运动 | 错误 | 多余 | 字段 |\n", encoding="utf-8")
            with self.assertRaises(ValueError):
                daily_rows(Path(directory))


if __name__ == "__main__":
    unittest.main()
