//
// Created by sickworm on 2018/1/21.
//

#ifndef WECHATJUMPHELPER_GRAPH_H
#define WECHATJUMPHELPER_GRAPH_H

#define TYPE_POINT 1
#define TYPE_LINE 2
#define TYPE_ELLIPSE 3

class Graph {
public:
    Graph(int type, void *object);
    int type;
    void *objecct;
};


#endif //WECHATJUMPHELPER_GRAPH_H
