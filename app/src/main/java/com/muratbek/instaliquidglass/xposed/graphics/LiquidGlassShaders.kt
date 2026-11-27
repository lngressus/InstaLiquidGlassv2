package com.muratbek.instaliquidglass.xposed.graphics

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi

object LiquidGlassShaders {

    private const val LIQUID_GLASS_AGSL = """
        uniform shader backdrop;
        uniform float2 resolution;
        uniform float cornerRadius;
        uniform float lensStrength;
        uniform float borderWidth;
        uniform float borderAlpha;
        uniform float4 tintColor;
        uniform float2 touchPos;
        uniform float touchIntensity;
        uniform float isDark;

        float sdRoundedBox(float2 p, float2 b, float r) {
            float2 q = abs(p) - b + r;
            return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
        }

        half4 main(float2 fragCoord) {
            float2 halfSize = resolution * 0.5;
            float2 p = fragCoord - halfSize;
            float r = min(cornerRadius, min(halfSize.x, halfSize.y));
            float dist = sdRoundedBox(p, halfSize, r);

            if (dist > 0.5) {
                return half4(0.0);
            }

            float rimZone = clamp(1.0 + dist / max(r * 0.65, 1.0), 0.0, 1.0);
            float lensCurve = pow(rimZone, 2.4) * lensStrength;

            float2 dir = length(p) > 0.001 ? normalize(p) : float2(0.0);
            float refractOffset = lensCurve * 14.0;

            float2 uvBase = fragCoord - dir * refractOffset;
            float2 uvR = clamp(uvBase - dir * (lensCurve * 2.2), float2(0.0), resolution - 1.0);
            float2 uvG = clamp(uvBase, float2(0.0), resolution - 1.0);
            float2 uvB = clamp(uvBase + dir * (lensCurve * 2.2), float2(0.0), resolution - 1.0);

            half4 sampled;
            sampled.r = backdrop.eval(uvR).r;
            sampled.g = backdrop.eval(uvG).g;
            sampled.b = backdrop.eval(uvB).b;
            sampled.a = 1.0;

            half3 glassRgb = mix(sampled.rgb, half3(tintColor.rgb), half(tintColor.a));

            float verticalSheen = clamp(1.0 - (fragCoord.y / max(resolution.y, 1.0)), 0.0, 1.0);
            float sheenBoost = pow(verticalSheen, 2.0) * (isDark > 0.5 ? 0.11 : 0.22);
            glassRgb += half3(sheenBoost);

            if (touchIntensity > 0.001) {
                float dTouch = distance(fragCoord, touchPos);
                float spot = exp(-dTouch * dTouch / (resolution.y * resolution.y * 0.9)) * touchIntensity * 0.26;
                glassRgb += half3(spot);
            }

            float edgeDist = abs(dist);
            float rimMask = 1.0 - smoothstep(0.0, max(borderWidth, 0.8), edgeDist);
            float topLight = clamp(0.65 - (p.y / max(halfSize.y, 1.0)) * 0.45, 0.25, 1.0);
            half3 rimColor = isDark > 0.5 ? half3(0.88, 0.94, 1.0) : half3(1.0, 1.0, 1.0);
            glassRgb = mix(glassRgb, rimColor, half(rimMask * borderAlpha * topLight));

            float aa = 1.0 - smoothstep(-1.0, 0.5, dist);
            return half4(glassRgb * half(aa), half(aa));
        }
    """

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun createLiquidGlassShader(): RuntimeShader? {
        return try {
            RuntimeShader(LIQUID_GLASS_AGSL)
        } catch (_: Throwable) {
            null
        }
    }
}
