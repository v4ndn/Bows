#version 330

#ifndef BOWS_TIMESTOP_GLSL
#define BOWS_TIMESTOP_GLSL

// ---- Bows: мир без красок у замороженных таймстопом (TimestopBoost.java) ----
//
// Таймстоп выдаёт замороженным ночное зрение; lightmap.fsh при нём ставит
// в тексель (0, 0) карты освещения альфу 254/255. Шейдеры мира читают этот
// флаг в вершинном шейдере и приглушают итоговый цвет во фрагментном.

float bows_timestop_flag(sampler2D lightMap) {
    return texelFetch(lightMap, ivec2(0, 0), 0).a < 0.999 ? 1.0 : 0.0;
}

vec3 bows_timestop_color(vec3 color, float amount) {
    float gray = dot(color, vec3(0.299, 0.587, 0.114));
    // почти без насыщенности, контраст к середине, лёгкий холодный оттенок
    vec3 muted = mix(vec3(gray), color, 0.2);
    muted = mix(vec3(0.5), muted, 0.75);
    muted = mix(muted, vec3(0.46, 0.50, 0.58), 0.12);
    return mix(color, muted, amount);
}

#endif
