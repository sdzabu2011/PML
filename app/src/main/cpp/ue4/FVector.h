#ifndef FVECTOR_H
#define FVECTOR_H

#include <math.h>

struct FVector {
    float X, Y, Z;

    FVector() : X(0), Y(0), Z(0) {}
    FVector(float x, float y, float z) : X(x), Y(y), Z(z) {}

    FVector operator+(const FVector& other) const { return FVector(X + other.X, Y + other.Y, Z + other.Z); }
    FVector operator-(const FVector& other) const { return FVector(X - other.X, Y - other.Y, Z - other.Z); }
    FVector operator*(float val) const { return FVector(X * val, Y * val, Z * val); }

    float Distance(const FVector& v) const {
        return sqrtf(powf(v.X - X, 2) + powf(v.Y - Y, 2) + powf(v.Z - Z, 2));
    }
};

struct FVector2D {
    float X, Y;
    FVector2D() : X(0), Y(0) {}
    FVector2D(float x, float y) : X(x), Y(y) {}
};

struct FMatrix {
    float M[4][4];
};

struct FTransform {
    FVector Translation;
    FVector Scale3D;
    // Quaternion Rotation would go here, simplified for padding
    float pad[4];
};

#endif
