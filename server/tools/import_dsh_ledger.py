"""Read dsh-im ledger.jsonl and emit SQL to stdout. No personal data is written to a file."""
import argparse
import json
import sys
from pathlib import Path

CAT_MAX = 50
CHANNEL_MAX = 30
MERCHANT_MAX = 100
NOTE_MAX = 500
SOURCE_MAX = 20
SOURCE_REF_MAX = 100


def sql_text(value: str) -> str:
    return "CONVERT(0x" + value.encode("utf-8").hex().upper() + " USING utf8mb4)"


def load_rows(path: Path):
    rows = []
    for lineno, raw in enumerate(path.read_text(encoding="utf-8-sig").splitlines(), 1):
        if not raw.strip():
            continue
        try:
            obj = json.loads(raw)
        except json.JSONDecodeError as exc:
            raise ValueError(f"第 {lineno} 行不是合法 JSON：{exc}") from None
        if not isinstance(obj, dict):
            raise ValueError(f"第 {lineno} 行不是 JSON 对象")
        rows.append(obj)
    return rows


def clip(value, limit):
    if value is None:
        return None
    return str(value)[:limit]


def normalize(obj):
    kind = obj.get("kind")
    if kind not in ("expense", "income"):
        raise ValueError(f"记录 {obj.get('id')} 的 kind 不是 expense/income")
    amount = round(float(obj["amount"]), 2)
    if amount <= 0:
        raise ValueError(f"记录 {obj.get('id')} 的金额必须大于 0")
    date = obj.get("date")
    if not date:
        raise ValueError(f"记录 {obj.get('id')} 缺少日期")
    return {
        "entry_type": "EXPENSE" if kind == "expense" else "INCOME",
        "amount": amount,
        "date": date,
        "time": clip(obj.get("time"), 8) or None,
        "category": clip(obj.get("category"), CAT_MAX) or "其他",
        "channel": clip(obj.get("channel"), CHANNEL_MAX) or None,
        "merchant": clip(obj.get("merchant"), MERCHANT_MAX) or None,
        "note": clip(obj.get("note"), NOTE_MAX) or None,
        "source": clip(obj.get("source"), SOURCE_MAX) or None,
        "status": 0 if obj.get("void") else 1,
        "source_ref": clip("dsh-im/ledger/" + str(obj.get("id")), SOURCE_REF_MAX),
    }


def emit_sql(rows, username):
    print("SET NAMES utf8mb4;")
    print("START TRANSACTION;")
    print("SET @import_user_id = (SELECT id FROM app_user WHERE username = " + sql_text(username) + ");")
    for r in rows:
        values = [
            "@import_user_id",
            "'" + r["entry_type"] + "'",
            f"{r['amount']:.2f}",
            "'" + r["date"] + "'",
            sql_text(r["time"]) if r["time"] else "NULL",
            sql_text(r["category"]),
            sql_text(r["channel"]) if r["channel"] else "NULL",
            sql_text(r["merchant"]) if r["merchant"] else "NULL",
            sql_text(r["note"]) if r["note"] else "NULL",
            sql_text(r["source"]) if r["source"] else "NULL",
            str(r["status"]),
            sql_text(r["source_ref"]),
        ]
        print("INSERT INTO ledger_entry (user_id, entry_type, amount, occurred_on, occurred_time, "
              "category_name, channel_name, merchant, note, source_type, status, source_ref) VALUES (" +
              ", ".join(values) + ") ON DUPLICATE KEY UPDATE id = id;")
    print("COMMIT;")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--ledger", type=Path, required=True)
    parser.add_argument("--username")
    parser.add_argument("--summary", action="store_true")
    args = parser.parse_args()
    if not args.ledger.is_file():
        parser.error("账本文件不存在")
    objs = load_rows(args.ledger)
    rows = [normalize(o) for o in objs]
    if args.summary:
        expense = sum(1 for r in rows if r["entry_type"] == "EXPENSE" and r["status"] == 1)
        income = sum(1 for r in rows if r["entry_type"] == "INCOME" and r["status"] == 1)
        voided = sum(1 for r in rows if r["status"] == 0)
        print(json.dumps({"total": len(rows), "expense": expense, "income": income, "void": voided},
                         ensure_ascii=True))
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
