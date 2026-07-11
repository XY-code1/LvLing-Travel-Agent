import type { VisionRecognizeVO } from '../types';
import { upload } from '../utils/request';

export function recognizeSpot(sessionNo: string, imagePath: string): Promise<VisionRecognizeVO> {
  return upload<VisionRecognizeVO>('/api/tourist/vision/recognize', imagePath, 'image', { sessionNo });
}
