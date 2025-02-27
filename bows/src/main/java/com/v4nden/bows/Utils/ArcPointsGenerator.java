package com.v4nden.bows.Utils;

import java.util.ArrayList;
import java.util.List;

public class ArcPointsGenerator {

    public static List<double[]> getArcPoints(double radius, double startAngle, double endAngle, int density) {
        List<double[]> points = new ArrayList<>();

        // Рассчитываем угловой шаг между точками
        double angleStep = (endAngle - startAngle) / (density - 1);

        // Генерируем точки на дуге
        for (int i = 0; i < density; i++) {
            double angle = startAngle + angleStep * i; // Текущий угол
            double x = 0 + radius * Math.cos(angle); // Координата X
            double y = 0 + radius * Math.sin(angle); // Координата Y
            points.add(new double[] { x, y });
        }

        return points;
    }
}