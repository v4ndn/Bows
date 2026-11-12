#version 330

// Can't moj_import in things used during startup, when resource packs don't exist.
// This is a copy of dynamicimports.glsl and projection.glsl
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};
layout(std140) uniform Projection {
    mat4 ProjMat;
};

in vec3 Position;
in vec4 Color;

out vec4 vertexColor;

// ---- Bows: фон панели участников (GamePanel.java, см. rendertype_text.vsh) ----
//
// Фон сайдбара — два чёрных полупрозрачных прямоугольника: строка заголовка
// (верх строк − 10 .. верх строк − 1) и строки (верх строк − 1 .. низ).
// По x они от (левый край − 2) до (ширина экрана − 1), и левый край известен,
// потому что ширина панели постоянна. Число строк n восстанавливается по y:
// низ = половина высоты + 3n, верх строк = половина высоты − 6n.
// Строка заголовка пустая — её прямоугольник схлопывается в линию.
const float PANEL_WIDTH = 150.0;
const float PANEL_MARGIN = 4.0;

bool isWhole(float value) {
    return abs(value - round(value)) < 0.01 && round(value) >= 1.0 && round(value) <= 15.0;
}

void main() {
    vec3 position = Position;

    if (ProjMat[2][3] == 0.0 && all(lessThan(Color.rgb, vec3(0.002))) && Color.a < 0.999) {
        // игра округляет размер GUI вверх, а в проекции он дробный
        float guiWidth = ceil(2.0 / ProjMat[0][0] - 0.001);
        float guiHeight = ceil(-2.0 / ProjMat[1][1] - 0.001);
        float halfHeight = floor(guiHeight / 2.0);
        float left = guiWidth - PANEL_WIDTH - 3.0;
        bool onEdge = abs(position.x - (guiWidth - 1.0)) < 0.01 || abs(position.x - (left - 2.0)) < 0.01;

        float lines = -1.0;
        bool titleTop = false;
        if (onEdge) {
            if (position.y > halfHeight) {
                float n = (position.y - halfHeight) / 3.0;
                if (isWhole(n)) lines = round(n);
            } else {
                float n = (halfHeight - 1.0 - position.y) / 6.0;
                float title = (halfHeight - 10.0 - position.y) / 6.0;
                if (isWhole(n)) {
                    lines = round(n);
                } else if (isWhole(title)) {
                    lines = round(title);
                    titleTop = true;
                }
            }
        }
        if (lines > 0.0) {
            float top = halfHeight - 6.0 * lines;
            if (titleTop) {
                position.y = top - 1.0;
            }
            position.xy += vec2(PANEL_MARGIN - left, PANEL_MARGIN - top);
        }
    }

    gl_Position = ProjMat * ModelViewMat * vec4(position, 1.0);

    vertexColor = Color;
}
