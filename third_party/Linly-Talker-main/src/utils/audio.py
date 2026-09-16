import librosa
import numpy as np
# import tensorflow as tf
from scipy import signal
from scipy.io import wavfile
from src.utils.hparams import hparams as hp

def load_wav(path, sr):
    # librosa.core.load can hang in the isolated Windows Wav2Lip environment
    # while scipy's native WAV reader is stable for the MVP input format.
    source_sr, wav = wavfile.read(path)
    is_integer = np.issubdtype(wav.dtype, np.integer)
    wav = wav.astype(np.float32)
    if is_integer:
        wav /= np.iinfo(np.int16).max
    if wav.ndim > 1:
        wav = wav.mean(axis=1)
    if source_sr != sr:
        wav = librosa.resample(wav, orig_sr=source_sr, target_sr=sr)
    return wav

def save_wav(wav, path, sr):
    wav *= 32767 / max(0.01, np.max(np.abs(wav)))
    #proposed by @dsmiller
    wavfile.write(path, sr, wav.astype(np.int16))

def save_wavenet_wav(wav, path, sr):
    librosa.output.write_wav(path, wav, sr=sr)

def preemphasis(wav, k, preemphasize=True):
    if preemphasize:
        return signal.lfilter([1, -k], [1], wav)
    return wav

def inv_preemphasis(wav, k, inv_preemphasize=True):
    if inv_preemphasize:
        return signal.lfilter([1], [1, -k], wav)
    return wav

def get_hop_size():
    hop_size = hp.hop_size
    if hop_size is None:
        assert hp.frame_shift_ms is not None
        hop_size = int(hp.frame_shift_ms / 1000 * hp.sample_rate)
    return hop_size

def linearspectrogram(wav):
    D = _stft(preemphasis(wav, hp.preemphasis, hp.preemphasize))
    S = _amp_to_db(np.abs(D)) - hp.ref_level_db
    
    if hp.signal_normalization:
        return _normalize(S)
    return S

def melspectrogram(wav):
    D = _stft(preemphasis(wav, hp.preemphasis, hp.preemphasize))
    S = _amp_to_db(_linear_to_mel(np.abs(D))) - hp.ref_level_db
    
    if hp.signal_normalization:
        return _normalize(S)
    return S

def _lws_processor():
    import lws
    return lws.lws(hp.n_fft, get_hop_size(), fftsize=hp.win_size, mode="speech")

def _stft(y):
    if hp.use_lws:
        return _lws_processor(hp).stft(y).T
    else:
        _, _, spectrum = signal.stft(
            y,
            fs=hp.sample_rate,
            window='hann',
            nperseg=hp.win_size,
            noverlap=hp.win_size - get_hop_size(),
            nfft=hp.n_fft,
            boundary='zeros',
            padded=True,
        )
        return spectrum

##########################################################
#Those are only correct when using lws!!! (This was messing with Wavenet quality for a long time!)
def num_frames(length, fsize, fshift):
    """Compute number of time frames of spectrogram
    """
    pad = (fsize - fshift)
    if length % fshift == 0:
        M = (length + pad * 2 - fsize) // fshift + 1
    else:
        M = (length + pad * 2 - fsize) // fshift + 2
    return M


def pad_lr(x, fsize, fshift):
    """Compute left and right padding
    """
    M = num_frames(len(x), fsize, fshift)
    pad = (fsize - fshift)
    T = len(x) + 2 * pad
    r = (M - 1) * fshift + fsize - T
    return pad, pad + r
##########################################################
#Librosa correct padding
def librosa_pad_lr(x, fsize, fshift):
    return 0, (x.shape[0] // fshift + 1) * fshift - x.shape[0]

# Conversions
_mel_basis = None

def _linear_to_mel(spectogram):
    global _mel_basis
    if _mel_basis is None:
        _mel_basis = _build_mel_basis()
    return np.dot(_mel_basis, spectogram)

def _build_mel_basis():
    assert hp.fmax <= hp.sample_rate // 2
    # Keep the librosa-compatible Slaney filterbank without importing
    # librosa.filters, which hangs in this Windows MVP environment.
    def hz_to_mel(freq):
        f_sp = 200.0 / 3
        mels = (freq - 0.0) / f_sp
        log_sp = np.log(6.4) / 27.0
        if np.isscalar(freq):
            return mels if freq < 1000.0 else 15.0 + np.log(freq / 1000.0) / log_sp
        return np.where(freq < 1000.0, mels, 15.0 + np.log(np.maximum(freq, 1e-12) / 1000.0) / log_sp)

    def mel_to_hz(mels):
        f_sp = 200.0 / 3
        freqs = 0.0 + f_sp * mels
        log_sp = np.log(6.4) / 27.0
        return np.where(mels < 15.0, freqs, 1000.0 * np.exp(log_sp * (mels - 15.0)))

    min_mel = hz_to_mel(hp.fmin)
    max_mel = hz_to_mel(hp.fmax)
    mel_points = mel_to_hz(np.linspace(min_mel, max_mel, hp.num_mels + 2))
    fft_freqs = np.linspace(0.0, hp.sample_rate / 2.0, hp.n_fft // 2 + 1)
    basis = np.zeros((hp.num_mels, hp.n_fft // 2 + 1), dtype=np.float32)
    for i in range(hp.num_mels):
        lower, center, upper = mel_points[i:i + 3]
        rising = (fft_freqs - lower) / max(center - lower, 1e-12)
        falling = (upper - fft_freqs) / max(upper - center, 1e-12)
        basis[i] = np.maximum(0.0, np.minimum(rising, falling))
        enorm = 2.0 / max(upper - lower, 1e-12)
        basis[i] *= enorm
    return basis

def _amp_to_db(x):
    min_level = np.exp(hp.min_level_db / 20 * np.log(10))
    return 20 * np.log10(np.maximum(min_level, x))

def _db_to_amp(x):
    return np.power(10.0, (x) * 0.05)

def _normalize(S):
    if hp.allow_clipping_in_normalization:
        if hp.symmetric_mels:
            return np.clip((2 * hp.max_abs_value) * ((S - hp.min_level_db) / (-hp.min_level_db)) - hp.max_abs_value,
                           -hp.max_abs_value, hp.max_abs_value)
        else:
            return np.clip(hp.max_abs_value * ((S - hp.min_level_db) / (-hp.min_level_db)), 0, hp.max_abs_value)
    
    assert S.max() <= 0 and S.min() - hp.min_level_db >= 0
    if hp.symmetric_mels:
        return (2 * hp.max_abs_value) * ((S - hp.min_level_db) / (-hp.min_level_db)) - hp.max_abs_value
    else:
        return hp.max_abs_value * ((S - hp.min_level_db) / (-hp.min_level_db))

def _denormalize(D):
    if hp.allow_clipping_in_normalization:
        if hp.symmetric_mels:
            return (((np.clip(D, -hp.max_abs_value,
                              hp.max_abs_value) + hp.max_abs_value) * -hp.min_level_db / (2 * hp.max_abs_value))
                    + hp.min_level_db)
        else:
            return ((np.clip(D, 0, hp.max_abs_value) * -hp.min_level_db / hp.max_abs_value) + hp.min_level_db)
    
    if hp.symmetric_mels:
        return (((D + hp.max_abs_value) * -hp.min_level_db / (2 * hp.max_abs_value)) + hp.min_level_db)
    else:
        return ((D * -hp.min_level_db / hp.max_abs_value) + hp.min_level_db)
