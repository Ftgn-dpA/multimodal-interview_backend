import os
import subprocess
import sys

import torch
import torchaudio
import numpy as np
from transformers import pipeline
#python3.9+cuda12.8版本torch

# 设置 huggingface 镜像（可选）
os.environ['HF_ENDPOINT'] = 'https://hf-mirror.com'
os.environ['HF_HOME'] = './models'

# 初始化模型，只加载一次
emotion_pipe = pipeline(
    "audio-classification",
    model="audeering/wav2vec2-large-robust-12-ft-emotion-msp-dim",
)


def extract_audio_ffmpeg(mp4_path, output_wav_path="extracted.wav"):
    command = [
        "D:/anaconda3/envs/interview/Library/bin/ffmpeg.exe",
        "-i", mp4_path,
        "-ac", "1",             # 单声道
        "-ar", "16000",         # 采样率 16kHz
        '-acodec', 'pcm_s16le',   # 16-bit PCM
        "-vn",  # 不要视频流
        "-y",  # 自动覆盖输出文件
        output_wav_path
    ]

    try:
        subprocess.run(command, check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        return output_wav_path
    except subprocess.CalledProcessError as e:
        print("FFmpeg 提取音频失败:", e.stderr.decode())
        raise


def analyze_emotion_interval(audio_path: str, segment_duration: int = 10, step_interval: int = 60):
    """
    每隔 step_interval 秒，截取一段 segment_duration 秒音频进行情感分析。
    返回平均得分、各段结果和时间戳。
    """
    waveform, sample_rate = torchaudio.load(audio_path)

    # 转为 16kHz 单声道
    # if sample_rate != 16000:
    #     waveform = torchaudio.transforms.Resample(orig_freq=sample_rate, new_freq=16000)(waveform)
    #     sample_rate = 16000
    # if waveform.shape[0] > 1:
    #     waveform = torch.mean(waveform, dim=0, keepdim=True)

    total_samples = waveform.shape[1]
    segment_len = segment_duration * sample_rate
    step_len = step_interval * sample_rate

    results = []

    for start in range(0, total_samples - segment_len + 1, step_len):
        end = start + segment_len
        segment = waveform[:, start:end]

        temp_file = "temp_segment.wav"
        torchaudio.save(temp_file, segment, sample_rate)

        result = emotion_pipe(temp_file)
        results.append(result)

        os.remove(temp_file)

    # 汇总平均得分
    scores_by_label = {"arousal": [], "valence": [], "dominance": []}
    for segment in results:
        for item in segment:
            scores_by_label[item["label"]].append(item["score"])

    avg_scores = {
        label: round(float(np.mean(scores)), 4)
        for label, scores in scores_by_label.items()
    }

    return avg_scores


# ✅ 主函数入口
if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("用法：python emotion_analysis_runner.py <音频文件路径>")
        sys.exit(1)

    audio_file = sys.argv[1]
    review='语音语调分析：'
    review2=''
    if not os.path.exists(audio_file):
        print(f"文件不存在：{audio_file}")
        sys.exit(1)
    avg= analyze_emotion_interval(extract_audio_ffmpeg(audio_file))
    if avg.get('arousal')<0.3:
        review2+='语调较负面;'
    if avg.get('valence') < 0.3:
        review2 += '声音较小;'
    if avg.get('dominance') < 0.4:
        review2 += '语调较不自信;'
    if review2=='':
        review2='语音语调无问题;'
    review+=review2
    print (review)
