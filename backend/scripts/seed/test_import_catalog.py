import io
import unittest
import urllib.error
import urllib.request
from unittest.mock import patch
from import_catalog import api_json

class ImportErrorsTest(unittest.TestCase):
    def test_login_failure_explains_correct_password_without_leaking_response(self):
        req=urllib.request.Request('https://example.test/api/auth/login',data=b'private-password')
        error=urllib.error.HTTPError(req.full_url,401,'',{},io.BytesIO(b'private-response'))
        with patch('urllib.request.urlopen',side_effect=error):
            with self.assertRaises(SystemExit) as raised: api_json(req,30,login=True)
        self.assertIn('App',str(raised.exception))
        self.assertNotIn('private',str(raised.exception))
    def test_import_permission_error_is_distinct_from_login_failure(self):
        req=urllib.request.Request('https://example.test/api/catalog/imported')
        error=urllib.error.HTTPError(req.full_url,403,'',{},io.BytesIO(b''))
        with patch('urllib.request.urlopen',side_effect=error):
            with self.assertRaises(SystemExit) as raised: api_json(req,30)
        self.assertIn('CATALOG_ADMIN_USER_ID',str(raised.exception))

    def test_image_rejection_shows_safe_reason_and_endpoint(self):
        req=urllib.request.Request('https://example.test/api/upload/image')
        body='{"code":400,"message":"请上传JPEG或PNG图片"}'.encode()
        error=urllib.error.HTTPError(req.full_url,400,'',{},io.BytesIO(body))
        with patch('urllib.request.urlopen',side_effect=error):
            with self.assertRaises(SystemExit) as raised: api_json(req,30)
        self.assertIn('请上传JPEG或PNG图片',str(raised.exception))
        self.assertIn('/api/upload/image',str(raised.exception))
