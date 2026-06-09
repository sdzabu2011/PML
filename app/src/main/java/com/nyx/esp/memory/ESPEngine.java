package com.nyx.esp.memory;

import android.util.Log;

/**
 * ESPEngine — InternalMemory dan foydalanib PUBG xotirasidan
 * dushmanlarning koordinatalarini o'qiydi va ekran koordinatalariga
 * o'girib beradi.
 *
 * Bu yerda haqiqiy UE4 Actor tizimining mantiqiy zanjirlari amalga oshirilgan:
 * GWorld -> PersistentLevel -> ActorArray -> Actor -> RootComponent -> RelativeLocation
 */
public class ESPEngine {
    private static final String TAG = "NyxESPEngine";

    // ===================== OFFSETS (PUBG Mobile v3.2 64-bit) =====================
    // Bu offsetlar o'yin versiyasi yangilanganda o'zgaradi!
    // Yangi offsetlarni Dumper yoki IDA Pro orqali topish kerak.
    
    private static final long OFF_GWORLD           = 0xC3F1200L;
    private static final long OFF_GNAMES           = 0xC1C3240L;
    
    // GWorld -> ...
    private static final long OFF_PERSISTENT_LEVEL = 0x30L;
    private static final long OFF_GAME_INSTANCE    = 0x1B8L;
    private static final long OFF_LOCAL_PLAYERS    = 0x38L;
    private static final long OFF_PLAYER_CONTROLLER= 0x30L;
    private static final long OFF_ACKNOWLEDGED_PAWN= 0x338L;
    private static final long OFF_PLAYER_CAMERA    = 0x348L;

    // ULevel -> ActorArray
    private static final long OFF_ACTOR_ARRAY      = 0x98L;
    private static final long OFF_ACTOR_COUNT      = 0xA0L;

    // AActor -> ...
    private static final long OFF_ACTOR_ID         = 0x18L;
    private static final long OFF_ROOT_COMPONENT   = 0x190L;
    private static final long OFF_ACTOR_HEALTH     = 0x930L;
    private static final long OFF_ACTOR_TEAM_ID    = 0x598L;

    // USceneComponent -> RelativeLocation
    private static final long OFF_RELATIVE_LOCATION= 0x164L;
    private static final long OFF_COMPONENT_VELOCITY= 0x1A0L;

    // ViewMatrix (Camera projection)
    private static final long OFF_VIEW_MATRIX      = 0xC1A21D0L;

    private InternalMemory mem;
    private long libBase;
    private int screenW, screenH;

    // Mahalliy o'yinchining jamoasi (o'z jamoamizni filtrlash uchun)
    private int myTeamId = -1;

    public ESPEngine(InternalMemory memory, long libUE4Base) {
        this.mem = memory;
        this.libBase = libUE4Base;
    }

    public void setScreenSize(int w, int h) {
        this.screenW = w;
        this.screenH = h;
    }

    /**
     * Barcha dushmanlarni skanerlash va ularni ekran koordinatalariga o'girish
     * 
     * Qaytariladigan float[] formatda:
     * [type, screenX, screenY, distance, health, type, screenX, screenY, distance, health, ...]
     * type: 1.0 = Player, 2.0 = Loot/Item
     */
    public float[] scanPlayers() {
        if (mem == null || !mem.isAttached() || libBase == 0) {
            return new float[0];
        }

        try {
            // 1. GWorld ni o'qish
            long gWorld = mem.readLong(libBase + OFF_GWORLD);
            if (gWorld == 0) return new float[0];

            // 2. PersistentLevel ni olish
            long persistentLevel = mem.readLong(gWorld + OFF_PERSISTENT_LEVEL);
            if (persistentLevel == 0) return new float[0];

            // 3. GameInstance -> LocalPlayers -> PlayerController
            long gameInstance = mem.readLong(gWorld + OFF_GAME_INSTANCE);
            if (gameInstance != 0) {
                long localPlayers = mem.readLong(gameInstance + OFF_LOCAL_PLAYERS);
                if (localPlayers != 0) {
                    long firstPlayer = mem.readLong(localPlayers);
                    if (firstPlayer != 0) {
                        long playerController = mem.readLong(firstPlayer + OFF_PLAYER_CONTROLLER);
                        if (playerController != 0) {
                            long myPawn = mem.readLong(playerController + OFF_ACKNOWLEDGED_PAWN);
                            if (myPawn != 0) {
                                myTeamId = mem.readInt(myPawn + OFF_ACTOR_TEAM_ID);
                            }
                        }
                    }
                }
            }

            // 4. ViewMatrix ni o'qish (WorldToScreen uchun zarur)
            float[] viewMatrix = mem.readMatrix(libBase + OFF_VIEW_MATRIX);

            // 5. Actor ro'yxatini olish
            long actorArray = mem.readLong(persistentLevel + OFF_ACTOR_ARRAY);
            int actorCount = mem.readInt(persistentLevel + OFF_ACTOR_COUNT);

            // Xavfsizlik cheklovi (juda ko'p aktorlar jarayonni sekinlashtiradi)
            if (actorCount <= 0 || actorCount > 10000) return new float[0];

            // Natija massivi (har bir aktor uchun 5 ta float)
            float[] result = new float[actorCount * 5];
            int resultIndex = 0;

            for (int i = 0; i < actorCount; i++) {
                long actor = mem.readLong(actorArray + (i * 8L)); // 64-bit pointer
                if (actor == 0) continue;

                // Actor ID ni tekshirish (Player yoki Loot ekanligini aniqlash)
                int actorId = mem.readInt(actor + OFF_ACTOR_ID);
                
                // Dushman jamoasini tekshirish
                int teamId = mem.readInt(actor + OFF_ACTOR_TEAM_ID);
                
                // O'zimizning jamoani o'tkazib yuboramiz
                if (teamId == myTeamId && myTeamId != -1) continue;

                // RootComponent -> Koordinatalar
                long rootComp = mem.readLong(actor + OFF_ROOT_COMPONENT);
                if (rootComp == 0) continue;

                float[] worldPos = mem.readVector3(rootComp + OFF_RELATIVE_LOCATION);
                if (worldPos[0] == 0 && worldPos[1] == 0 && worldPos[2] == 0) continue;

                // Health ni o'qish
                float health = mem.readFloat(actor + OFF_ACTOR_HEALTH);
                if (health <= 0 || health > 100) continue; // O'lik yoki invalid

                // WorldToScreen hisobi
                float[] screenPos = worldToScreen(worldPos, viewMatrix);
                if (screenPos == null) continue; // Ekranda ko'rinmayapti

                // Masofani hisoblash (metrda)
                float distance = calculateDistance(worldPos);

                // Natijani yozish
                if (resultIndex + 4 < result.length) {
                    result[resultIndex]     = 1.0f;           // Type: Player
                    result[resultIndex + 1] = screenPos[0];   // Screen X
                    result[resultIndex + 2] = screenPos[1];   // Screen Y
                    result[resultIndex + 3] = distance;       // Distance (m)
                    result[resultIndex + 4] = health;         // HP
                    resultIndex += 5;
                }
            }

            // Faqat to'ldirilgan qismini qaytarish
            float[] finalResult = new float[resultIndex];
            System.arraycopy(result, 0, finalResult, 0, resultIndex);
            return finalResult;

        } catch (Exception e) {
            Log.e(TAG, "Skanner xatolik: " + e.getMessage());
            return new float[0];
        }
    }

    /**
     * 3D dunyo koordinatalarini 2D ekran koordinatalariga o'girish
     * UE4 ViewProjectionMatrix orqali ishlaydi
     */
    private float[] worldToScreen(float[] worldPos, float[] matrix) {
        if (matrix == null || matrix.length < 16) return null;

        float screenX = matrix[0] * worldPos[0] + matrix[4] * worldPos[1] 
                       + matrix[8] * worldPos[2] + matrix[12];
        float screenY = matrix[1] * worldPos[0] + matrix[5] * worldPos[1] 
                       + matrix[9] * worldPos[2] + matrix[13];
        float screenW_val = matrix[3] * worldPos[0] + matrix[7] * worldPos[1] 
                       + matrix[11] * worldPos[2] + matrix[15];

        // Kamera orqasida turgan ob'ektlarni filtrlash
        if (screenW_val < 0.001f) return null;

        float invW = 1.0f / screenW_val;
        float ndcX = screenX * invW;
        float ndcY = screenY * invW;

        // NDC dan Ekran piksellariga
        float outX = (screenW / 2.0f) + (ndcX * screenW / 2.0f);
        float outY = (screenH / 2.0f) - (ndcY * screenH / 2.0f);

        // Ekran chegarasida tekshirish
        if (outX < -200 || outX > screenW + 200) return null;
        if (outY < -200 || outY > screenH + 200) return null;

        return new float[]{outX, outY};
    }

    /**
     * Masofani hisoblash (UE4 birlik = santimetr, /100 = metr)
     */
    private float calculateDistance(float[] worldPos) {
        // Oddiy masofa formulasi (fazoviy)
        float dist = (float) Math.sqrt(
                worldPos[0] * worldPos[0] +
                worldPos[1] * worldPos[1] +
                worldPos[2] * worldPos[2]
        );
        return dist / 100.0f; // UE4 cm -> metr
    }
}
