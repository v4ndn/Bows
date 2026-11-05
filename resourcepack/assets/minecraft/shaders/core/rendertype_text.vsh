#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:globals.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

uniform sampler2D Sampler2;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

// ---- Bows: анимированный HUD-текст над прицелом ----
//
// Плагин шлёт тайтл, у которого цвет текста и цвет тени — метки (HudText.java).
// Такие вершины переносятся из места ванильного тайтла в точку над прицелом.
//
// Прогресс анимации — альфа тайтла: клиент сам плавно (с учётом доли тика)
// ведёт её 0 -> 1 за fadeIn, держит 1 на время stay и ведёт 1 -> 0 за fadeOut.
const ivec3 HUD_TEXT_MARKER = ivec3(254, 1, 0);
const ivec3 HUD_SHADOW_MARKER = ivec3(254, 2, 0);
const vec3 HUD_COLOR = vec3(1.0, 0.95, 0.88);

// ванильный тайтл: масштаб 4, строка рисуется с y = -10 от центра экрана
const float VANILLA_TITLE_SCALE = 4.0;
const float VANILLA_TITLE_CENTER_Y = -26.0;

const float HUD_SCALE = 2.0;
// отступ над прицелом в пикселях окна
const float HUD_OFFSET_PIXELS = 200.0;
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

        // пиксели окна -> GUI-единицы
        float offset = HUD_OFFSET_PIXELS * guiHeight / ScreenSize.y;
        vec2 vanillaCenter = vec2(guiWidth / 2.0, guiHeight / 2.0 + VANILLA_TITLE_CENTER_Y);
        vec2 target = vec2(guiWidth / 2.0, guiHeight / 2.0 - offset + (1.0 - t) * HUD_SLIDE);
        float scale = HUD_SCALE / VANILLA_TITLE_SCALE * mix(0.85, 1.0, t);

        position.xy = target + (position.xy - vanillaCenter) * scale;
        vertexColor = vec4(isHudShadow ? HUD_COLOR * 0.25 : HUD_COLOR, t);
    }

    gl_Position = ProjMat * ModelViewMat * vec4(position, 1.0);

    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
    texCoord0 = UV0;
}
