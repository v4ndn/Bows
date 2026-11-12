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

// ---- Bows: элемент статуса на месте полоски опыта (StatusBar.java) ----
//
// Строка actionbar из шрифта bows:hud шириной 182px, как полоска опыта, поэтому
// по x она уже стоит над ней (обе по центру экрана). По y actionbar рисуется
// с guiHeight − 72, полоска опыта — около guiHeight − 29: сдвиг вниз постоянный.
const ivec3 STATUS_MARKER = ivec3(254, 7, 0);
const float STATUS_SHIFT_Y = 42.0;

// ---- Bows: панель участников в левом верхнем углу (GamePanel.java) ----
//
// Это сайдбар скорборда. Цвет текста строк — метка (254, вид, число строк):
// вид 3 — белый, 5 — радар, 6 — серый (хп и высота), 16..31 — цвет команды (код цвета 0..15).
// Все строки одной ширины PANEL_WIDTH, поэтому левый край сайдбара известен:
// ширина экрана − PANEL_WIDTH − 3; верх строк — половина высоты экрана − 6 × строк
// (как в ванильном Hud.displayScoreboardSidebar). Фон переносит gui.vsh.
const int PANEL_MARKER_RED = 254;
const float PANEL_WIDTH = 210.0;
const float PANEL_MARGIN = 4.0;
const vec3 PANEL_RADAR_COLOR = vec3(233.0, 114.0, 24.0) / 255.0;
const vec3 PANEL_GRAY_COLOR = vec3(166.0) / 255.0;
const vec3 LEGACY_COLORS[16] = vec3[16](
    vec3(0.0, 0.0, 0.0), vec3(0.0, 0.0, 0.667), vec3(0.0, 0.667, 0.0), vec3(0.0, 0.667, 0.667),
    vec3(0.667, 0.0, 0.0), vec3(0.667, 0.0, 0.667), vec3(1.0, 0.667, 0.0), vec3(0.667, 0.667, 0.667),
    vec3(0.333, 0.333, 0.333), vec3(0.333, 0.333, 1.0), vec3(0.333, 1.0, 0.333), vec3(0.333, 1.0, 1.0),
    vec3(1.0, 0.333, 0.333), vec3(1.0, 0.333, 1.0), vec3(1.0, 1.0, 0.333), vec3(1.0, 1.0, 1.0));

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

    if (rgb == STATUS_MARKER && ProjMat[2][3] == 0.0) {
        position.y += STATUS_SHIFT_Y;
        vertexColor = vec4(1.0, 1.0, 1.0, Color.a) * texelFetch(Sampler2, UV2 / 16, 0);
    }

    bool isPanel = rgb.r == PANEL_MARKER_RED && rgb.g >= 3 && rgb.g <= 31 && rgb.b >= 1 && rgb.b <= 15;
    if (isPanel && ProjMat[2][3] == 0.0) {
        // игра округляет размер GUI вверх, а в проекции он дробный
        float guiWidth = ceil(2.0 / ProjMat[0][0] - 0.001);
        float guiHeight = ceil(-2.0 / ProjMat[1][1] - 0.001);
        float lines = float(rgb.b);
        float left = guiWidth - PANEL_WIDTH - 3.0;
        float top = floor(guiHeight / 2.0) - 6.0 * lines;
        position.xy += vec2(PANEL_MARGIN - left, PANEL_MARGIN - top);

        vec3 color = vec3(1.0);
        if (rgb.g == 5) {
            color = PANEL_RADAR_COLOR;
        } else if (rgb.g == 6) {
            color = PANEL_GRAY_COLOR;
        } else if (rgb.g >= 16) {
            color = LEGACY_COLORS[rgb.g - 16];
        }
        vertexColor = vec4(color, Color.a) * texelFetch(Sampler2, UV2 / 16, 0);
    }

    gl_Position = ProjMat * ModelViewMat * vec4(position, 1.0);

    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
    texCoord0 = UV0;
}
