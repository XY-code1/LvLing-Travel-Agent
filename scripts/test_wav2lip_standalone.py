import importlib.util
import os
import shutil
import sys
import time
import traceback
import types
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / 'third_party' / 'Linly-Talker-main'
IMAGE = ROOT / 'inputs' / 'girl.png'
AUDIO_SOURCE = next((ROOT / 'src' / 'flagged' / 'output').glob('*.wav'))
AUDIO = Path(__file__).resolve().parents[1] / 'outputs' / 'digital-human' / 'phase4f-test.wav'
OUTPUT = Path(__file__).resolve().parents[1] / 'outputs' / 'digital-human' / 'standalone-wav2lip-test.mp4'
MODEL = ROOT / 'checkpoints' / 'wav2lip_gan.pth'

def load_wav2lip():
    sys.path.insert(0, str(ROOT))
    pkg = types.ModuleType('TFG')
    pkg.__path__ = [str(ROOT / 'TFG')]
    sys.modules['TFG'] = pkg
    spec = importlib.util.spec_from_file_location('TFG.Wav2Lip', ROOT / 'TFG' / 'Wav2Lip.py')
    module = importlib.util.module_from_spec(spec)
    sys.modules['TFG.Wav2Lip'] = module
    spec.loader.exec_module(module)
    return module.Wav2Lip

def main():
    ffmpeg_bin = Path(r'D:\conda\envs\travel-digital-human\Library\bin')
    os.environ['PATH'] = str(ffmpeg_bin) + os.pathsep + os.environ.get('PATH', '')
    AUDIO.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    if not AUDIO.exists():
        shutil.copyfile(AUDIO_SOURCE, AUDIO)
    Wav2Lip = load_wav2lip()
    started = time.perf_counter()
    model = Wav2Lip(str(MODEL))
    result = model.predict(str(IMAGE), str(AUDIO), batch_size=1, fps=25, enhance=False)
    shutil.copyfile(ROOT / result, OUTPUT)
    print(f'output={OUTPUT}')
    print(f'elapsed_seconds={time.perf_counter() - started:.2f}')

if __name__ == '__main__':
    try:
        main()
    except BaseException:
        traceback.print_exc()
        raise
