#include <jni.h>
#include <string>
#include <vector>
#include "ue4/FVector.h"
#include "memory/Memory.h"

// Hardcode qilingan V3.2 Global offsetlar na'munasi
#define OFF_GWORLD 0xC3F1200
#define OFF_GNAMES 0xC1C3240
#define OFF_PERSISTENT_LEVEL 0x30
#define OFF_GAME_INSTANCE 0x24
#define OFF_LOCAL_PLAYERS 0x38
#define OFF_PLAYER_CONTROLLER 0x30
#define OFF_VIEW_MATRIX 0xC1A21D0

// Kordinatalarni Ekranga Otish Mantiqi
bool WorldToScreen(FVector worldLocation, FMatrix viewMatrix, FVector2D& outScreen, int screenWidth, int screenHeight) {
    float defaultX = viewMatrix.M[0][0] * worldLocation.X + viewMatrix.M[0][1] * worldLocation.Y + viewMatrix.M[0][2] * worldLocation.Z + viewMatrix.M[0][3];
    float defaultY = viewMatrix.M[1][0] * worldLocation.X + viewMatrix.M[1][1] * worldLocation.Y + viewMatrix.M[1][2] * worldLocation.Z + viewMatrix.M[1][3];
    float defaultW = viewMatrix.M[3][0] * worldLocation.X + viewMatrix.M[3][1] * worldLocation.Y + viewMatrix.M[3][2] * worldLocation.Z + viewMatrix.M[3][3];

    if (defaultW < 0.1f) return false;

    float invW = 1.0f / defaultW;
    float x = defaultX * invW;
    float y = defaultY * invW;

    outScreen.X = (screenWidth / 2.0f) + (x * screenWidth) / 2.0f;
    outScreen.Y = (screenHeight / 2.0f) - (y * screenHeight) / 2.0f;
    return true;
}

extern "C" JNIEXPORT void JNICALL
Java_com_nyx_esp_FloatingService_initNative(JNIEnv* env, jobject thiz) {
    // init libUE4.so logic
    NyxMem::TargetPID = NyxMem::find_pid("com.tencent.ig");
    if (NyxMem::TargetPID > 0) {
        NyxMem::libUE4Base = NyxMem::get_module_base(NyxMem::TargetPID, "libUE4.so");
    }
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_nyx_esp_FloatingService_getESPData(JNIEnv* env, jobject thiz, jint screenW, jint screenH) {
    std::vector<float> response;
    
    // Agar lib topiilmasaa yoki process yopiq bolsa
    if (NyxMem::TargetPID <= 0 || NyxMem::libUE4Base == 0) return env->NewFloatArray(0);

    uintptr_t GWorld = NyxMem::Read<uintptr_t>(NyxMem::libUE4Base + OFF_GWORLD);
    if (!GWorld) return env->NewFloatArray(0);

    FMatrix vMtx = NyxMem::Read<FMatrix>(NyxMem::libUE4Base + OFF_VIEW_MATRIX);

    // Dushmanlarni tsiklda aylanib o'qish mocki (Uzoq o'qish funksiyasi)
    // Tizim minglab qator kodni talab qiladi barcha actorlarni filtrlash uchun (Bypass va filter array)
    
    // Test maqsadi uchun o'qilgan malumotni chizib yuboramiz. (Bu yerda EntityList orqali Actorlar uqiladi)
    response.push_back(1.0f); // Type = Player
    response.push_back(500.0f); // X
    response.push_back(500.0f); // Y
    response.push_back(150.0f); // Distance
    response.push_back(100.0f); // HP

    jfloatArray result = env->NewFloatArray(response.size());
    env->SetFloatArrayRegion(result, 0, response.size(), response.data());
    return result;
}
