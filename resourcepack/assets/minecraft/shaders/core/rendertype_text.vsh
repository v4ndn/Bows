#version 150

#moj_import <minecraft:fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

// ---- Bows: анимированный HUD-текст над прицелом ----
//
// Плагин шлёт тайтл, у которого цвет текста и цвет тени — метки (HudText.java).
// Такие вершины переносятся из места ванильного тайтла в точку над прицелом.
//
// Своего времени у текстового шейдера в 1.21.5 нет, поэтому прогресс анимации —
// альфа тайтла: клиент сам плавно (с учётом доли тика) ведёт её 0 -> 1 за fadeIn,
// держит 1 на время stay и ведёт 1 -> 0 за fadeOut.
const ivec3 HUD_TEXT_MARKER = ivec3(254, 1, 0);
const ivec3 HUD_SHADOW_MARKER = ivec3(254, 2, 0);
const vec3 HUD_COLOR = vec3(1.0, 0.95, 0.88);

// ванильный тайтл: масштаб 4, строка рисуется с y = -10 от центра экрана
const float VANILLA_TITLE_SCALE = 4.0;
const float VANILLA_TITLE_CENTER_Y = -26.0;

const float HUD_SCALE = 2.0;
// 200 пикселей над прицелом при высоте окна 1080; в шейдере текста нет размера
// окна, только размер GUI, поэтому отступ — доля высоты экрана
const float HUD_OFFSET_FRACTION = 200.0 / 1080.0;
// на сколько GUI-пикселей текст выезжает снизу при появлении
const float HUD_SLIDE = 10.0;

float easeOutCubic(float x) {
    return 1.0 - pow(1.0 - x, 3.0);
}

void main() {
    vec3 position = Position;
    vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);

    ivec3 rgb = ivec3(round(Color.rgb * 255.0));
    bool isHudText = rgb == HUD_TEXT_MARKER;
    bool isHudShadow = rgb == HUD_SHADOW_MARKER;
    // только GUI: у ортографической проекции ProjMat[2][3] == 0
    if ((isHudText || isHudShadow) && ProjMat[2][3] == 0.0) {
        float guiWidth = 2.0 / ProjMat[0][0];
        float guiHeight = -2.0 / ProjMat[1][1];
        float t = easeOutCubic(clamp(Color.a, 0.0, 1.0));

        vec2 vanillaCenter = vec2(guiWidth / 2.0, guiHeight / 2.0 + VANILLA_TITLE_CENTER_Y);
        vec2 target = vec2(guiWidth / 2.0,
                guiHeight / 2.0 - guiHeight * HUD_OFFSET_FRACTION + (1.0 - t) * HUD_SLIDE);
        float scale = HUD_SCALE / VANILLA_TITLE_SCALE * mix(0.85, 1.0, t);

        position.xy = target + (position.xy - vanillaCenter) * scale;
        vertexColor = vec4(isHudShadow ? HUD_COLOR * 0.25 : HUD_COLOR, t);
    }

    gl_Position = ProjMat * ModelViewMat * vec4(position, 1.0);

    vertexDistance = fog_distance(Position, FogShape);
    texCoord0 = UV0;
}
