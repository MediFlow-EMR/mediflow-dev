import io
import json
import os
import sys
import unittest
from unittest.mock import MagicMock, patch

# Unit tests do not need AWS credentials or a local boto3 installation.
sys.modules.setdefault("boto3", MagicMock())
import lambda_function as app


class DrugCacheTests(unittest.TestCase):
    def call(self, items, code="00"):
        response = io.BytesIO(json.dumps({
            "header": {"resultCode": code}, "body": {"items": items}
        }).encode())
        with patch.dict(os.environ, {
            "CACHE_BUCKET": "test-bucket", "CACHE_PREFIX": "drug-cache/",
            "DRUG_API_SERVICE_KEY": "test-key"
        }), patch.object(app, "urlopen", return_value=response), \
                patch.object(app.boto3, "client") as client:
            result = app.lambda_handler({"item_seq": "202602327"}, None)
            return result, client

    def test_cache_exact_product(self):
        result, client = self.call([{"ITEM_SEQ": "202602327", "CANCEL_NAME": "정상"}])
        self.assertEqual(result["status"], "CACHED")
        self.assertEqual(client.return_value.put_object.call_args.kwargs["Key"],
                         "drug-cache/202602327.json")

    def test_wrong_product_not_stored(self):
        result, client = self.call([{"ITEM_SEQ": "202602328"}])
        self.assertEqual(result["status"], "NOT_FOUND")
        client.assert_not_called()

    def test_inactive_not_stored(self):
        result, client = self.call([{"ITEM_SEQ": "202602327", "CANCEL_NAME": "취하"}])
        self.assertEqual(result["status"], "INACTIVE")
        client.assert_not_called()

    def test_api_error_not_cached(self):
        with self.assertRaises(RuntimeError):
            self.call([], code="30")

    def test_reject_invalid_identifier(self):
        with self.assertRaises(ValueError):
            app.lambda_handler({"item_seq": "../secret"}, None)


if __name__ == "__main__":
    unittest.main()
