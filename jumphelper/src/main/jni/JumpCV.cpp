//
// Created by chope on 2018/1/10.
//

#include "JumpCV.h"

#include <cmath>
#include <algorithm>

#pragma clang diagnostic push
#pragma ide diagnostic ignored "UnusedValue"
#define PI 3.1415926

using namespace std;
using namespace cv;


JumpCV::JumpCV(int width, int height, float density) {
    g_width = width;
    g_height = height;
    g_density = density;
}

JumpCV::~JumpCV() {
    g_debugGraphs.clear();
}

bool JumpCV::findChess(IN Mat img, OUT Point &chessPoint) {
    int h = 100;
    int s = 60;
    int v = 60;
    int h2 = 135;
    int s2 = 130;
    int v2 = 110;
    // TODO 适配屏幕分辨率
    int chessMinArea = 5000;
    int chessMaxArea = 11000;

    // 转成 HSV，方便滤色
    cvtColor(img, hsv, COLOR_RGB2HSV);
    // 过滤棋子颜色
    inRange(hsv, Scalar(h, s, v), Scalar(h2, s2, v2), mask);

    vector<vector<Point> > contours;
    findContours(mask, contours, CV_RETR_TREE, CV_CHAIN_APPROX_SIMPLE);
    LOGD("findChess find chess has %d contours", contours.size());
    if (contours.size() == 0) {
        LOGW("findChess no contours available");
        return false;
    }

    // 找棋子底座
    double chessArea = 0;
    vector<Point> chessContour;
    for (int i = 0; i < contours.size(); i++) {
        vector<Point> contour = contours[i];
        double area = contourArea(contour);
        if (area < chessMinArea || area > chessMaxArea) {
            continue;
        }
        if (chessArea < area) {
            chessArea = area;
            chessContour = contour;
        }
    }
    if (chessArea == 0) {
        LOGW("findChess find chess contours failed");
        return false;
    }

    // 找中心点，即棋子左右两侧最宽处的中心
    Point leftCenterPoint = Point(99999, 99999);
    Point rightCenterPoint = Point(0, 0);
    for (int i = 0; i < chessContour.size(); i++) {
        Point point = chessContour[i];
        if (leftCenterPoint.x > point.x) {
            leftCenterPoint = point;
        } else if (leftCenterPoint.x == point.x && leftCenterPoint.y < point.y) {
            leftCenterPoint = point;
        }

        if (rightCenterPoint.x < point.x) {
            rightCenterPoint = point;
        } else if (rightCenterPoint.x == point.x and rightCenterPoint.y < point.y) {
            rightCenterPoint = point;
        }
    }
    chessPoint.x = ((leftCenterPoint.x + rightCenterPoint.x) / 2);
    chessPoint.y = ((leftCenterPoint.y + rightCenterPoint.y) / 2);
    g_debugGraphs.push_back(new Point(chessPoint));
    LOGI("findChess chess center.x=%d, center.y=%d", chessPoint.x, chessPoint.y);
    return true;
}

int getAngle(Point c, Point p1, Point p2) {
    Point p1p2 = Point(p1.x - c.x, p1.y - c.y);
    Point p2p3 = Point(p2.x - c.x, p2.y - c.y);
    double lp1p2 = sqrt(p1p2.x * p1p2.x + p1p2.y * p1p2.y);
    double lp2p3 = sqrt(p2p3.x * p2p3.x + p2p3.y * p2p3.y);
    int p1p2xp2p3 = p1p2.x * p2p3.x + p1p2.y * p2p3.y;
    double cosA = p1p2xp2p3 / (lp1p2 * lp2p3);
    return (int) (acos(cosA) / PI * 180);
}


bool cmp(const Vec4i &a, const Vec4i &b) {
    return a[1] < b[1];
}

bool JumpCV::findPlatformSquare(IN Mat img, OUT Point &platformPoint) {
    int threshold1 = 20;
    int threshold2 = 85;
    int minLineLength = 100;
    int maxLineGap = 10;
    int rho = 10;
    int theta = 180;
    int minLineHeadGap = 10;
    int minLineTailDistance = 100;
    int standardAngle = 130;
    int angleDeviation = 20;
    int threshold = 50;

    GaussianBlur(img, blu, Size(7, 7), 2, 2);
    Canny(blu, binary, threshold1, threshold2);

    // 找直线
    vector<Vec4i> lines;
    HoughLinesP(binary, lines, rho / 10, PI / theta, threshold, minLineLength, maxLineGap);
    LOGD("findPlatformSquare has %d lines", lines.size());

    // 直线从上到下排序
    for (int i = 0; i < lines.size(); i++) {
        Vec4i line = lines[i];
        if (line[1] < line[3]) {
            int t;
            t = line[0];
            line[0] = line[2];
            line[2] = t;
            t = line[1];
            line[1] = line[3];
            line[3] = t;
        }
    }
    sort(lines.begin(), lines.end(), cmp);
    
    // 寻找两头相接的直线，且夹角范围为 standardAngle +- angleDeviation
    vector<Vec4i> foundLines;
    Vec4i lastLine = Vec4i(0, 0, 0, 0);
    for (int i = 0; i < lines.size(); i++) {
        Vec4i line = lines[i];
        LOGD("%d %d %d",
             abs(lastLine[1] - line[1]),
             abs(lastLine[0] - line[0]),
             abs(lastLine[2] - line[2]));
        if (abs(lastLine[0] - line[0]) < minLineHeadGap &&
            abs(lastLine[1] - line[1]) < minLineHeadGap &&
            abs(lastLine[2] - line[2]) > minLineTailDistance) {
            if ((line[2] - line[0] == 0) || (lastLine[2] - line[0] == 0)) {
                continue;
            }
            Point linesNode;
            linesNode.x = (lastLine[0] + line[0]) / 2;
            linesNode.y = (lastLine[1] + line[1]) / 2;
            int degree = getAngle(linesNode,
                Point(lastLine[2], lastLine[3]), Point(line[2], line[3]));
            LOGD("findPlatformSquare found node of lines with degree %d", degree);
            if ((degree < standardAngle - angleDeviation) ||
                (degree > standardAngle + angleDeviation)) {
                continue;
            }
            foundLines.push_back(lastLine);
            foundLines.push_back(line);
            break;
        }
        lastLine = line;
    }
    if (foundLines.size() == 0) {
        LOGI("findPlatformSquare found line failed");
        return false;
    }

    // 计算中点
    Vec4i line1 = foundLines[0];
    Vec4i line2 = foundLines[1];
    if (abs(line1[0] - line2[0]) < 2) {
        platformPoint.x = (line1[2] + line2[2]) / 2;
        platformPoint.y = (line1[3] + line2[3]) / 2;
    } else {
        platformPoint.x = (line1[0] + line2[0]) / 2;
        platformPoint.y = (line1[1] + line1[1]) / 2;
    }
    LOGI("findPlatformSquare center.x=%d, center.y=%d", platformPoint.x, platformPoint.y);
    if (DEBUG_TYPE & (DEBUG_ALL_DEST | DEBUG_SQUARE)) {
        g_debugGraphs.push_back(new Point(platformPoint));
        g_debugGraphs.push_back(new Vec4i(foundLines[0]));
        g_debugGraphs.push_back(new Vec4i(foundLines[1]));
    }
    return true;
}

bool JumpCV::findPlatformCircle(IN Mat img, OUT Point &platformPoint) {
    int threshold1 = 20;
    int threshold2 = 35;
    // TODO 适配屏幕
    int minArea = 10000;
    int maxArea = 400000;

    Canny(img, binary, threshold1, threshold2);
    vector<vector<Point> > contours;
    findContours(binary, contours, RETR_TREE, CHAIN_APPROX_SIMPLE);

    vector<RotatedRect> fitEllipses;
    for (int i = 0; i < contours.size(); i++) {
        vector<Point> contour = contours[i];
        if (contour.size() < 10) {
            continue;
        }
        RotatedRect ellipse = fitEllipse(contour);
        if (ellipse.size.width / ellipse.size.height < 0.5) {
            continue;
        }
        if (ellipse.angle < 80) {
            continue;
        }
        double area = PI * ellipse.size.height * ellipse.size.width;
        if (area < minArea || area > maxArea) {
            continue;
        }
        fitEllipses.push_back(ellipse);
    }
    if (fitEllipses.size() == 0) {
        LOGD("findPlatformCircle find no fit ellipses");
        return false;
    }

    RotatedRect highestEllipse;
    double highestPointY = 99999;
    for (int i = 0; i < fitEllipses.size(); i++) {
        RotatedRect ellipse = fitEllipses[i];
        double topPointY = ellipse.center.y - ellipse.size.width * cos(PI *  ellipse.angle / 360)
                            - ellipse.size.height * sin(PI * ellipse.angle / 360);
        if (highestPointY > topPointY) {
            highestEllipse = ellipse;
            highestPointY = topPointY;
        }
    }
    platformPoint.x = (int) (highestEllipse).center.x;
    platformPoint.y = (int) (highestEllipse).center.y;
    LOGI("findPlatformCircle center x=%d, y=%d", platformPoint.x, platformPoint.y);
    if (DEBUG_TYPE & (DEBUG_ALL_DEST  | DEBUG_CIRCLE)) {
        g_debugGraphs.push_back(new Point(platformPoint));
        g_debugGraphs.push_back(new RotatedRect(highestEllipse));
    }
    return true;
}

bool JumpCV::findWhitePoint(IN Mat img, OUT Point &whitePoint) {
    int h = 0;
    int s = 0;
    int v = 244;
    int h2 = 0;
    int s2 = 0;
    int v2 = 245;
    int pointMinArea = 2000;
    int pointMaxArea = 3000;
    float ellipseMinScale = 0.3f;
    float ellipseMinAngle = 80;

    // 过滤颜色
    cvtColor(img, hsv, COLOR_RGB2HSV);
    Scalar lowerWhite = Scalar(h, s, v);
    Scalar upperWhite = Scalar(h2, s2, v2);
    inRange(hsv, lowerWhite, upperWhite, mask);

    vector<vector<Point> > contours;
    findContours(mask, contours, RETR_TREE, CHAIN_APPROX_SIMPLE);
    LOGD("findWhitePoint has %d contours", contours.size());
    if (contours.size() == 0) {
        LOGI("findWhitePoint has no contour");
        return false;
    }

    // 找白点，要求为横椭圆，面积 pointMinArea ~ pointMaxArea
    for (int i = 0; i < contours.size(); i++) {
        vector<Point> contour = contours[i];
        if (contour.size() < 10) {
            continue;
        }
        RotatedRect ellipse = fitEllipse(contour);
        // 长宽比
        if (ellipse.size.width / ellipse.size.height < ellipseMinScale) {
            continue;
        }
        if (ellipse.angle < ellipseMinAngle) {
            continue;
        }
        double area = PI * ellipse.size.width * ellipse.size.height;
        if (area < pointMinArea || area > pointMaxArea) {
            continue;
        }
        whitePoint.x = (int) ellipse.center.x;
        whitePoint.y = (int) ellipse.center.y;
        LOGI("white point x=%d, y=%d", whitePoint.x, whitePoint.y);
        if (DEBUG_TYPE & (DEBUG_ALL_DEST | DEBUG_WHITE_POINT)) {
            g_debugGraphs.push_back(new Point(whitePoint));
        }
        return true;
    }

    LOGD("findWhitePoint not white point");
    return false;
}

bool JumpCV::findPlatform(IN cv::Mat img, OUT cv::Point &platformPoint) {
    Point whitePoint;
    Point circlePoint;
    Point squarePoint;
    bool squareRet;
    bool circleRet;

    if(findWhitePoint(img, whitePoint)) {
        platformPoint.x = whitePoint.x;
        platformPoint.y = whitePoint.y;
        goto succeed;
    }

    squareRet = findPlatformSquare(img, squarePoint);
    circleRet = findPlatformCircle(img, circlePoint);
    if (circleRet && squareRet) {
        if (circlePoint.y < squarePoint.y) {
            platformPoint.x = circlePoint.x;
            platformPoint.y = circlePoint.y;
        } else {
            platformPoint.x = squarePoint.x;
            platformPoint.y = squarePoint.y;
        }
        goto succeed;
    } else if (squareRet) {
        platformPoint.x = squarePoint.x;
        platformPoint.y = squarePoint.y;
        goto succeed;
    } else if (circleRet) {
        platformPoint.x = circlePoint.x;
        platformPoint.y = circlePoint.y;
        goto succeed;
    }

    LOGW("platformPoint not found");
    return false;

    succeed:
    LOGI("platformPoint x=%d, y=%d", platformPoint.x, platformPoint.y);
    return true;
}

vector<void *> JumpCV::getGraphs() {
    return g_debugGraphs;
}

void JumpCV::clearGraphs() {
    g_debugGraphs.clear();
}

#pragma clang diagnostic pop