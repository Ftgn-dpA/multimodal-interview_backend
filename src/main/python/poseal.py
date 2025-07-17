import cv2
import mediapipe as mp
import numpy as np
import argparse
#python3.9

def analyze_video(video_path, frame_interval=5):
    mp_pose = mp.solutions.pose
    pose = mp_pose.Pose(static_image_mode=False, model_complexity=1)
    cap = cv2.VideoCapture(video_path)
    shoulder_track = []
    wrist_track = []
    frame_count = 0

    while cap.isOpened():
        ret, frame = cap.read()
        if not ret:
            break

        frame_count += 1
        if frame_count % frame_interval != 0:
            continue  # 跳帧处理

        image = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        results = pose.process(image)

        if results.pose_landmarks:
            landmarks = results.pose_landmarks.landmark

            l_shoulder = landmarks[11]
            r_shoulder = landmarks[12]
            l_wrist = landmarks[15]
            r_wrist = landmarks[16]

            shoulder_track.append([(l_shoulder.x + r_shoulder.x)/2, (l_shoulder.y + r_shoulder.y)/2])
            wrist_track.append([(l_wrist.x + r_wrist.x)/2, (l_wrist.y + r_wrist.y)/2])

    cap.release()


    shoulder_track = np.array(shoulder_track)
    wrist_track = np.array(wrist_track)

    shoulder_var = np.var(shoulder_track, axis=0).mean()
    wrist_movement = np.linalg.norm(np.diff(wrist_track, axis=0), axis=1).mean()


    return round(shoulder_var, 4), round(wrist_movement, 4)

def main():
    parser = argparse.ArgumentParser(description="视频肢体语言分析")
    parser.add_argument('--video', type=str, required=True, help="视频文件路径")
    args = parser.parse_args()

    shoulder_var, wrist_movement= analyze_video(args.video)
    review = '肢体语言分析：'
    review2=''
    if shoulder_var > 0.4:
        review2 += '身体晃动太多;'
    if wrist_movement < 0.4:
        review2 += '肢体语言较少;'
    if review2=='':
        review2='肢体规范正常;'
    review+=review2
    print(review)

if __name__ == "__main__":
    main()
