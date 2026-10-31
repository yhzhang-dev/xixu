"""Read daily dsh-im tables and emit SQL to stdout. No personal data is written to a file.

All imported rows remain NOTE. A source row mentioning exercise is not automatically
promoted to a completed workout, because plans, summaries and uncertain rep ranges coexist.
"""

import argparse
import json
import re
import sys
from collections import Counter
from pathlib import Path


DAY_NAME = re.compile(r"^\d{4}-\d{2}-\d{2}\.md$")


def daily_rows(log_dir: Path):
    result = []
    for path in sorted(log_dir.glob("????-??-??.md")):
        if not DAY_NAME.fullmatch(path.name):
            continue
        in_records = False
        row_number = 0
        for line_number, line in enumerate(path.read_text(encoding="utf-8-sig").splitlines(), 1):
            if line.startswith("## 生活记录"):
                in_records = True
                continue
            if in_records and line.startswith("## "):
                in_records = False
            if not in_records or not line.startswith("|") or line.startswith("|---"):
                continue
            cells = [part.strip() for part in line.strip("|").split("|")]
            if cells[0] == "时间":
                continue
            if len(cells) != 4:
                raise ValueError(f"{path.name}:{line_number} 的表格不是四列，已停止导入")
            time_label, category, record, remark = cells
            if not category or not record:
                raise ValueError(f"{path.name}:{line_number} 缺少类别或记录，已停止导入")
            row_number += 1
            title = f"{category} · {time_label}"[:100]
            source_ref = f"dsh-im/log/{path.name}#life-{row_number:03d}"
            fields = (category, time_label, title, record, remark, source_ref)
            limits = (30, 50, 100, 2000, 2000, 150)
            if any(len(value) > limit for value, limit in zip(fields, limits)):
                raise ValueError(f"{path.name}:{line_number} 超出数据库字段长度，已停止导入")
            result.append((path.stem, *fields))
    return result


def sql_text(value: str):
    return "CONVERT(0x" + value.encode("utf-8").hex().upper() + " USING utf8mb4)"


def emit_sql(rows, username):
    print("SET NAMES utf8mb4;")
    print("START TRANSACTION;")
    print("SET @import_user_id = (SELECT id FROM app_user WHERE username = " + sql_text(username) + ");")
    for date, category, time_label, title, record, remark, source_ref in rows:
        values = ["@import_user_id", "'NOTE'", sql_text(category), f"'{date}'",
                  sql_text(time_label), sql_text(title), sql_text(record),
                  sql_text(remark) if remark else "NULL", "'IMPORT'", sql_text(source_ref)]
        print("INSERT INTO life_log_entry (user_id, entry_type, category_name, occurred_on, "
              "time_label, title, content, remark, source_type, source_ref) VALUES (" +
              ", ".join(values) + ") ON DUPLICATE KEY UPDATE id = id;")
    print("COMMIT;")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--log-dir", type=Path, required=True)
    parser.add_argument("--username")
    parser.add_argument("--summary", action="store_true")
    args = parser.parse_args()
    if not args.log_dir.is_dir():
        parser.error("日志目录不存在")
    rows = daily_rows(args.log_dir)
    if not rows:
        parser.error("没有找到每日生活记录表")
    if args.summary:
        counts = Counter(row[1] for row in rows)
        print(json.dumps({"days": len({row[0] for row in rows}), "rows": len(rows),
                          "categories": counts}, ensure_ascii=True))
        return
    if not args.username:
        parser.error("生成 SQL 时需要 --username")
    emit_sql(rows, args.username)


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError) as error:
        print(error, file=sys.stderr)
        sys.exit(1)
