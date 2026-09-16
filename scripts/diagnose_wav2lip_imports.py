import subprocess
import sys
from pathlib import Path

PY = r'D:\conda\envs\travel-digital-human\python.exe'
ROOT = Path(r'E:\Guido-main\third_party\Linly-Talker-main')
PROJECT = Path(r'E:\Guido-main')
LOG = Path(r'E:\Guido-main\outputs\digital-human\wav2lip-import-diagnostics.txt')

steps = [
    ('A torch', 'import torch; print(torch.__version__, flush=True)'),
    ('B cv2', 'import cv2; print(cv2.__version__, flush=True)'),
    ('C numpy', 'import numpy; print(numpy.__version__, flush=True)'),
    ('D scipy', 'import scipy; print(scipy.__version__, flush=True)'),
    ('E librosa', 'import librosa; print(librosa.__version__, flush=True)'),
    ('F face_detection', 'import face_detection; print("OK", flush=True)'),
    ('G FaceAlignment', 'from face_detection import FaceAlignment; print("OK", flush=True)'),
    ('H S3FD init', 'import face_detection; x=face_detection.FaceAlignment(face_detection.LandmarksType._2D, flip_input=False, device="cuda"); print("OK", flush=True)'),
    ('I S3FD checkpoint', 'import torch; p=r"checkpoints/hub/checkpoints/s3fd-619a316812.pth"; x=torch.load(p, map_location="cpu", weights_only=False); print(type(x).__name__, flush=True)'),
    ('J src.models', 'import src.models; print("OK", flush=True)'),
    ('K Wav2Lip checkpoint', 'import torch; p=r"checkpoints/wav2lip_gan.pth"; x=torch.load(p, map_location="cpu", weights_only=False); print(list(x.keys())[:3], flush=True)'),
    ('L TFG.Wav2Lip direct', 'import importlib.util,sys,types; pkg=types.ModuleType("TFG"); pkg.__path__=["TFG"]; sys.modules["TFG"]=pkg; s=importlib.util.spec_from_file_location("TFG.Wav2Lip", "TFG/Wav2Lip.py"); m=importlib.util.module_from_spec(s); s.loader.exec_module(m); print("OK", flush=True)'),
    ('M S3FD detect CUDA', 'import cv2,face_detection,numpy as np; im=cv2.imread("inputs/girl.png"); d=face_detection.FaceAlignment(face_detection.LandmarksType._2D, flip_input=False, device="cuda"); print(d.get_detections_for_batch(np.array([im])), flush=True)'),
    ('N Wav2Lip constructor', 'import importlib.util,sys,types; pkg=types.ModuleType("TFG"); pkg.__path__=["TFG"]; sys.modules["TFG"]=pkg; s=importlib.util.spec_from_file_location("TFG.Wav2Lip", "TFG/Wav2Lip.py"); m=importlib.util.module_from_spec(s); s.loader.exec_module(m); x=m.Wav2Lip("checkpoints/wav2lip_gan.pth"); print("OK", flush=True)'),
    ('O audio load', 'from src.utils import audio; x=audio.load_wav(r"E:/Guido-main/outputs/digital-human/phase4f-test.wav",16000); print(len(x), flush=True)'),
    ('P audio mel', 'from src.utils import audio; x=audio.load_wav(r"E:/Guido-main/outputs/digital-human/phase4f-test.wav",16000); y=audio.melspectrogram(x); print(y.shape, flush=True)'),
]

LOG.parent.mkdir(parents=True, exist_ok=True)
with LOG.open('w', encoding='utf-8') as out:
    for name, code in steps:
        out.write(f'===== {name} =====\nCOMMAND: {PY} -c {code}\n')
        out.flush()
        try:
            r = subprocess.run([PY, '-u', '-c', code], cwd=ROOT, capture_output=True, text=True, timeout=120)
            out.write(f'EXIT_CODE: {r.returncode}\nSTDOUT:\n{r.stdout}\nSTDERR:\n{r.stderr}\n')
            out.flush()
            print(f'{name}: exit={r.returncode}', flush=True)
            if r.returncode != 0:
                print(f'FIRST_FAILURE={name}', flush=True)
                break
        except subprocess.TimeoutExpired as e:
            out.write(f'TIMEOUT: {e}\n')
            print(f'{name}: TIMEOUT', flush=True)
            break
print(f'LOG={LOG}', flush=True)
