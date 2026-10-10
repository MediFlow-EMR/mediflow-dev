"""Cache one MFDS permit product by its exact item identifier (no patient data)."""
import json
import os
import re
from datetime import datetime, timezone
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import urlopen

import boto3

ENDPOINT = (
    "https://apis.data.go.kr/1471000/DrugPrdtPrmsnInfoService08/"
    "getDrugPrdtPrmsnDtlInq08"
)


def lambda_handler(event, context):
    item_seq = str(event.get("item_seq", ""))
    if not re.fullmatch(r"[0-9]{9}", item_seq):
        raise ValueError("item_seq must be a nine-digit MFDS product identifier")

    bucket = os.environ["CACHE_BUCKET"]
    prefix = os.environ.get("CACHE_PREFIX", "drug-cache/")
    if prefix != "drug-cache/":
        raise ValueError("CACHE_PREFIX must be drug-cache/")
    query = urlencode({
        "serviceKey": os.environ["DRUG_API_SERVICE_KEY"],
        "item_seq": item_seq,
        "pageNo": 1,
        "numOfRows": 10,
        "type": "json",
    })
    try:
        with urlopen(ENDPOINT + "?" + query, timeout=25) as response:
            raw = response.read(2_000_001)
        if len(raw) > 2_000_000:
            raise ValueError("MFDS response exceeded size limit")
        data = json.loads(raw)
    except (HTTPError, URLError, TimeoutError, OSError, ValueError):
        # Never expose the request URL: it contains the service key.
        raise RuntimeError("MFDS request failed; check API approval and service key") from None

    if str((data.get("header") or {}).get("resultCode")) != "00":
        raise RuntimeError("MFDS API returned an unsuccessful result")
    items = (data.get("body") or {}).get("items") or []
    if not isinstance(items, list):
        raise RuntimeError("Unexpected MFDS items format")
    matches = [item for item in items
               if str(item.get("ITEM_SEQ")) == item_seq]
    if len(matches) != 1:
        return {"status": "NOT_FOUND", "item_seq": item_seq, "stored": False}
    item = matches[0]
    if item.get("CANCEL_NAME") not in (None, "", "정상"):
        return {"status": "INACTIVE", "item_seq": item_seq, "stored": False}

    document = {
        "schema_version": 1,
        "source": "MFDS_DRUG_PERMIT",
        "item_seq": item_seq,
        "fetched_at": datetime.now(timezone.utc).isoformat(),
        "item": item,
    }
    key = prefix + item_seq + ".json"
    boto3.client("s3").put_object(
        Bucket=bucket,
        Key=key,
        Body=json.dumps(document, ensure_ascii=False).encode("utf-8"),
        ContentType="application/json; charset=utf-8",
        ServerSideEncryption="AES256",
    )
    return {"status": "CACHED", "item_seq": item_seq, "key": key, "stored": True}
