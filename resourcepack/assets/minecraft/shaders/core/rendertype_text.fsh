#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
flat in ivec2 overlayEffect;
in vec2 overlayUv;

out vec4 fragColor;

// ---- Bows: полноэкранные эффекты (ScreenEffect.java, см. rendertype_text.vsh) ----
// Номера — как в ScreenEffect: 1 таймстоп, 2 рейдж, 3 финал, 4 мало хп, 5 невидимость.

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

// слой поверх: rgb и непрозрачность
vec4 over(vec4 below, vec3 color, float alpha) {
    alpha = clamp(alpha, 0.0, 1.0);
    float a = alpha + below.a * (1.0 - alpha);
    vec3 rgb = a > 0.0 ? (color * alpha + below.rgb * below.a * (1.0 - alpha)) / a : vec3(0.0);
    return vec4(rgb, a);
}

vec4 effect(int id, float strength) {
    float seconds = GameTime * 1200.0;
    vec2 uv = overlayUv;
    // расстояние от центра с поправкой на соотношение сторон: 0 — центр, ~1 — углы
    vec2 centered = (uv - 0.5) * vec2(ScreenSize.x / ScreenSize.y, 1.0) * 2.0;
    float edge = length(centered) / length(vec2(ScreenSize.x / ScreenSize.y, 1.0));
    float grain = hash(floor(uv * ScreenSize / 2.0) + floor(seconds * 24.0)) - 0.5;
    vec4 color = vec4(0.0);

    if (id == 1) {
        // таймстоп: холодная серая пелена, тёмно-синяя виньетка, зерно и медленная рябь у краёв
        color = over(color, vec3(0.62, 0.67, 0.76), 0.24);
        float ripple = sin(edge * 28.0 - seconds * 1.6) * 0.5 + 0.5;
        color = over(color, vec3(0.08, 0.10, 0.18), smoothstep(0.35, 1.0, edge) * 0.55 + ripple * smoothstep(0.55, 1.0, edge) * 0.08);
        color = over(color, vec3(grain > 0.0 ? 1.0 : 0.0), abs(grain) * 0.10);
    } else if (id == 2) {
        // рейдж: пульсирующая красная виньетка
        float pulse = 0.75 + 0.25 * sin(seconds * 9.0);
        color = over(color, vec3(0.75, 0.05, 0.02), smoothstep(0.25, 1.0, edge) * 0.65 * pulse);
    } else if (id == 3) {
        // финал: красно-оранжевое свечение по краям
        float pulse = 0.85 + 0.15 * sin(seconds * 2.0);
        color = over(color, vec3(0.92, 0.28, 0.06), smoothstep(0.55, 1.05, edge) * 0.5 * pulse);
    } else if (id == 4) {
        // мало хп: тёмно-красная рамка, бьётся как сердце
        float beat = pow(max(sin(seconds * 6.0), 0.0), 6.0);
        color = over(color, vec3(0.28, 0.0, 0.0), smoothstep(0.45, 1.0, edge) * (0.45 + 0.35 * beat));
    } else if (id == 5) {
        // невидимость: лёгкая голубоватая дымка и зерно
        color = over(color, vec3(0.70, 0.85, 0.95), 0.06 + smoothstep(0.5, 1.0, edge) * 0.18);
        color = over(color, vec3(grain > 0.0 ? 1.0 : 0.0), abs(grain) * 0.12);
    }
    color.a *= strength;
    return color;
}

void main() {
    if (overlayEffect.x > 0) {
        fragColor = effect(overlayEffect.x, float(overlayEffect.y) / 255.0);
        if (fragColor.a < 0.004) {
            discard;
        }
        return;
    }
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    if (color.a < 0.1) {
        discard;
    }
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
