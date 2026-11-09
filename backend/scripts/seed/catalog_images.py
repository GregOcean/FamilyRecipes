"""Prepare upload-compatible derivatives without changing downloaded source files."""
import hashlib
import re
import shutil
import subprocess
from pathlib import Path


def prepare_image(source, cache_dir):
    source = Path(source)
    digest = hashlib.sha256(source.read_bytes()).hexdigest()
    cache_dir = Path(cache_dir)
    cache_dir.mkdir(parents=True, exist_ok=True)
    target = cache_dir / (digest + '-upload-v1.png')
    if target.is_file() and target.stat().st_size:
        return target, 'image/png'
    sips = shutil.which('sips')
    if sips:
        info = subprocess.run([sips, '-g', 'format', '-g', 'pixelWidth', '-g', 'pixelHeight', str(source)], capture_output=True, text=True, check=True).stdout
        def field(key):
            match = re.search(r'^\s*' + key + r':\s*(\S+)', info, re.M)
            if not match: raise ValueError('Cannot read image ' + source.name)
            return match[1]
        fmt, width, height = field('format').lower(), int(field('pixelWidth')), int(field('pixelHeight'))
        if fmt in ('jpeg', 'png') and width * height <= 20_000_000 and source.stat().st_size < 10 * 1024 * 1024:
            return source, 'image/' + fmt
        temp = target.with_suffix('.tmp.png')
        try:
            command = [sips, '-s', 'format', 'png']
            if width * height > 20_000_000: command += ['-Z', '1600']
            subprocess.run(command + [str(source), '--out', str(temp)], capture_output=True, check=True)
            if temp.stat().st_size >= 10 * 1024 * 1024:
                subprocess.run([sips, '-Z', '1600', str(temp)], capture_output=True, check=True)
            if temp.stat().st_size >= 10 * 1024 * 1024: raise ValueError('Image remains too large: ' + source.name)
            temp.replace(target)
        finally:
            temp.unlink(missing_ok=True)
    else:
        try:
            from PIL import Image, ImageOps
        except ImportError:
            raise SystemExit('图片转换需要 macOS sips 或 Pillow。当前 Python 环境安装 Pillow 后重试。') from None
        with Image.open(source) as original:
            if original.format in ('JPEG', 'PNG') and original.width * original.height <= 20_000_000 and source.stat().st_size < 10 * 1024 * 1024:
                return source, 'image/jpeg' if original.format == 'JPEG' else 'image/png'
            picture = ImageOps.exif_transpose(original).convert('RGBA')
            if picture.width * picture.height > 20_000_000: picture.thumbnail((1600, 1600))
            temp = target.with_suffix('.tmp.png')
            try:
                picture.save(temp, format='PNG')
                if temp.stat().st_size >= 10 * 1024 * 1024:
                    picture.thumbnail((1600, 1600)); picture.save(temp, format='PNG')
                if temp.stat().st_size >= 10 * 1024 * 1024: raise ValueError('Image remains too large: ' + source.name)
                temp.replace(target)
            finally:
                temp.unlink(missing_ok=True)
    return target, 'image/png'
