#!/usr/bin/env python3
"""Encode captured browser frames as repository demo GIFs."""
from pathlib import Path
import sys
from PIL import Image

frames_root = Path(sys.argv[1])
output = Path(__file__).resolve().parents[1] / 'frontend/public/assets'
for name in ('login', 'patientdashboard', 'nursingrecord', 'ordertab', 'aihandover'):
    paths = sorted((frames_root / name).glob('*.png'))
    if len(paths) < 2:
        raise ValueError(f'Missing capture sequence: {name}')
    captures = [Image.open(path).convert('RGB') for path in paths]
    canvas_size = (max(frame.width for frame in captures), max(frame.height for frame in captures))
    frames = []
    for capture in captures:
        canvas = Image.new('RGB', canvas_size, capture.getpixel((0, 0)))
        canvas.paste(capture, ((canvas.width - capture.width) // 2, 0))
        frames.append(canvas.quantize(colors=256))
    target = output / f'{name}.gif'
    frames[0].save(target, save_all=True, append_images=frames[1:], duration=1800, loop=0, optimize=True, disposal=2)
    print(f'{name}: {len(frames)} frames, {target.stat().st_size:,} bytes')
