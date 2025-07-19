import argparse

#视频微表情分析代码
from feat import Detector
import torch
#python3.9+cuda12.8版本torch

def score_affinity(au_means1):
    # 亲和力 = 微笑相关 AU（AU06 + AU12）平均值
    score = (au_means1.get("AU06", 0) + au_means1.get("AU12", 0)) / 2
    return map_score(score)

def score_tension(au_means2):
    # 紧张度 = 眉头紧皱/眼部收缩（AU04, AU07）
    score = (au_means2.get("AU04", 0) + au_means2.get("AU07", 0)) / 2
    return 5 - map_score(score)  # 紧张越高，分数越低

def score_emotional_stability(au_data):
    # 情绪波动 = AU 方差大小
    variance = au_data.var().mean()
    return 5 - map_variance_score(variance)

def map_score(val):
    # 将 AU 强度映射为 1-5 分数（可调节阈值）
    if val > 0.75:
        return 5
    elif val > 0.5:
        return 4
    elif val > 0.3:
        return 3
    elif val > 0.1:
        return 2
    else:
        return 1

def map_variance_score(val):
    # 方差大代表情绪波动大
    if val < 0.01:
        return 5
    elif val < 0.03:
        return 4
    elif val < 0.06:
        return 3
    elif val < 0.1:
        return 2
    else:
        return 1

# 检查GPU是否可用
def analyze_facial_video(inputvideo_path):
    device = 'cuda' if torch.cuda.is_available() else 'cpu'
    print(f"Using {device} device")

    # 初始化检测器并移动到GPU
    detector = Detector(device=device)

    review1 = '面部分析：'
    video_prediction = detector.detect_video(
        video_path=inputvideo_path, skip_frames=40, face_detection_threshold=0.95, batch_size=3
    )
    pose_rx = video_prediction.poses['Pitch'].mean()
    au_means = video_prediction.aus.mean()
    score_a = score_affinity(au_means)
    score_t = score_tension(au_means)
    score_e = score_emotional_stability(video_prediction.aus)
    emotion = video_prediction.emotions.mean().idxmax()
    print(pose_rx)
    if pose_rx < 4:
        review1 += '头部角度偏低;'
    review1 += f'亲和度得分（满分5分）:{score_a};抗压得分（满分5分）:{score_t};情绪控制（满分5分）:{score_e};面部情绪为{emotion}'
    return review1

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Analyze facial expressions in a video.")
    parser.add_argument("--video", type=str, help="Path to the input video file")
    args = parser.parse_args()
    import feat
    print(feat.__file__)
    review=analyze_facial_video(args.video)
    print(review)



