#ifndef VIDEOPLAYER_AVPACKET_QUEUE_H
#define VIDEOPLAYER_AVPACKET_QUEUE_H
#include <pthread.h>

typedef struct AVPacketQueue{
    int size;
    void ** packets;
    int next_to_write;
    int next_to_read;
}AVPacketQueue;

AVPacketQueue* queue_init(int size);
void queue_free(AVPacketQueue *queue);
void* queue_push(AVPacketQueue* queue, pthread_mutex_t* mutex, pthread_cond_t* cond);
void* queue_pop(AVPacketQueue* queue, pthread_mutex_t* mutex, pthread_cond_t* cond);

#endif //VIDEOPLAYER_AVPACKET_QUEUE_H