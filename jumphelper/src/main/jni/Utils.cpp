


// This file is part of OpenCV project.
// It is subject to the license terms in the LICENSE file found in the top-level directory
// of this distribution and at http://opencv.org/license.html

// modified by sickworm

#include "Utils.h"

#include "opencv2/core.hpp"
#include "opencv2/imgproc.hpp"

#include <android/bitmap.h>

//#include "common.h"


#ifdef __cplusplus
extern "C" {
#endif

using namespace cv;

Mat tmp;

/*
 * Class:     org_opencv_android_Utils
 * Method:    void nBitmapToMat2(Bitmap b, long m_addr, boolean unPremultiplyAlpha)
 *
 * memory optimized by sickworm
 */

void bitmapToMat2(JNIEnv * env, jclass, jobject bitmap, jlong m_addr, jboolean needUnPremultiplyAlpha)
{
    AndroidBitmapInfo  info;
    void*              pixels = 0;
    Mat&               dst = *((Mat*)m_addr);

    try {
        LOGD("nBitmapToMat");
        CV_Assert( AndroidBitmap_getInfo(env, bitmap, &info) >= 0 );
        CV_Assert( info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 ||
                   info.format == ANDROID_BITMAP_FORMAT_RGB_565 );
        CV_Assert( AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0 );
        CV_Assert( pixels );

        if (dst.cols != info.width || dst.rows != info.height || dst.type() != CV_8UC4) {
            dst.create(info.height, info.width, CV_8UC4);
        }
        if( info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 )
        {
            LOGD("nBitmapToMat: RGBA_8888 -> CV_8UC4");
            if (tmp.cols != info.width || tmp.rows != info.height || tmp.type() != CV_8UC4) {
                tmp.release();
                tmp = Mat(info.height, info.width, CV_8UC4, pixels);
            } else {
                tmp.data = (uchar *) pixels;
            }
            if(needUnPremultiplyAlpha)  {
                cvtColor(tmp, dst, COLOR_mRGBA2RGBA);
            } else {
                tmp.copyTo(dst);
            }
        } else {
            // info.format == ANDROID_BITMAP_FORMAT_RGB_565
            LOGD("nBitmapToMat: RGB_565 -> CV_8UC4");
            if (tmp.cols != info.width || tmp.rows != info.height || tmp.type() != CV_8UC2) {
                tmp = Mat(info.height, info.width, CV_8UC2, pixels);
            } else {
                tmp.data = (uchar *) pixels;
            }
            cvtColor(tmp, dst, COLOR_BGR5652RGBA);
        }
        AndroidBitmap_unlockPixels(env, bitmap);
        return;
    } catch(const cv::Exception& e) {
        AndroidBitmap_unlockPixels(env, bitmap);
        LOGE("nBitmapToMat catched cv::Exception: %s", e.what());
        jclass je = env->FindClass("org/opencv/core/CvException");
        if(!je) je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, e.what());
        return;
    } catch (...) {
        AndroidBitmap_unlockPixels(env, bitmap);
        LOGE("nBitmapToMat catched unknown exception (...)");
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "Unknown exception in JNI code {nBitmapToMat}");
        return;
    }
}

/*
 * Class:     org_opencv_android_Utils
 * Method:    void nMatToBitmap2(long m_addr, Bitmap b, boolean premultiplyAlpha)
 *
 * nothing changed
 */
void matToBitmap2(JNIEnv * env, jclass, jlong m_addr, jobject bitmap, jboolean needPremultiplyAlpha)
{
    AndroidBitmapInfo  info;
    void*              pixels = 0;
    Mat&               src = *((Mat*)m_addr);

    try {
        LOGD("nMatToBitmap");
        CV_Assert( AndroidBitmap_getInfo(env, bitmap, &info) >= 0 );
        CV_Assert( info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 ||
                   info.format == ANDROID_BITMAP_FORMAT_RGB_565 );
        CV_Assert( src.dims == 2 && info.height == (uint32_t)src.rows && info.width == (uint32_t)src.cols );
        CV_Assert( src.type() == CV_8UC1 || src.type() == CV_8UC3 || src.type() == CV_8UC4 );
        CV_Assert( AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0 );
        CV_Assert( pixels );
        if( info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 )
        {
            Mat tmp(info.height, info.width, CV_8UC4, pixels);
            if(src.type() == CV_8UC1)
            {
                LOGD("nMatToBitmap: CV_8UC1 -> RGBA_8888");
                cvtColor(src, tmp, COLOR_GRAY2RGBA);
            } else if(src.type() == CV_8UC3){
                LOGD("nMatToBitmap: CV_8UC3 -> RGBA_8888");
                cvtColor(src, tmp, COLOR_RGB2RGBA);
            } else if(src.type() == CV_8UC4){
                LOGD("nMatToBitmap: CV_8UC4 -> RGBA_8888");
                if(needPremultiplyAlpha) cvtColor(src, tmp, COLOR_RGBA2mRGBA);
                else src.copyTo(tmp);
            }
        } else {
            // info.format == ANDROID_BITMAP_FORMAT_RGB_565
            Mat tmp(info.height, info.width, CV_8UC2, pixels);
            if(src.type() == CV_8UC1)
            {
                LOGD("nMatToBitmap: CV_8UC1 -> RGB_565");
                cvtColor(src, tmp, COLOR_GRAY2BGR565);
            } else if(src.type() == CV_8UC3){
                LOGD("nMatToBitmap: CV_8UC3 -> RGB_565");
                cvtColor(src, tmp, COLOR_RGB2BGR565);
            } else if(src.type() == CV_8UC4){
                LOGD("nMatToBitmap: CV_8UC4 -> RGB_565");
                cvtColor(src, tmp, COLOR_RGBA2BGR565);
            }
        }
        AndroidBitmap_unlockPixels(env, bitmap);
        return;
    } catch(const cv::Exception& e) {
        AndroidBitmap_unlockPixels(env, bitmap);
        LOGE("nMatToBitmap catched cv::Exception: %s", e.what());
        jclass je = env->FindClass("org/opencv/core/CvException");
        if(!je) je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, e.what());
        return;
    } catch (...) {
        AndroidBitmap_unlockPixels(env, bitmap);
        LOGE("nMatToBitmap catched unknown exception (...)");
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "Unknown exception in JNI code {nMatToBitmap}");
        return;
    }
}

#ifdef  __cplusplus
}
#endif