package com.example.flybaitau.generator;

import com.example.flybaitau.model.Waypoint;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class KmzGenerator {

    public File generate(List<Waypoint> waypoints, File outputDir,
                         String missionName) throws IOException {
        String wpml     = buildWpml(waypoints);
        String template = buildTemplate();

        File kmzFile = new File(outputDir, missionName + ".kmz");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(kmzFile))) {
            addEntry(zos, "wpmz/waylines.wpml", wpml);
            addEntry(zos, "wpmz/template.kml",  template);
        }
        return kmzFile;
    }

    public File generatePreviewKml(List<Waypoint> waypoints,
                                   File outputDir,
                                   String missionName) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n");
        sb.append("<Document>\n");
        sb.append("  <name>").append(missionName).append("</name>\n");
        sb.append("  <Style id=\"routeStyle\">\n");
        sb.append("    <LineStyle><color>ffff5722</color><width>3</width></LineStyle>\n");
        sb.append("  </Style>\n");
        sb.append("  <Style id=\"wpStyle\">\n");
        sb.append("    <IconStyle><color>ff2196F3</color><scale>0.6</scale></IconStyle>\n");
        sb.append("  </Style>\n");
        sb.append("  <Placemark>\n");
        sb.append("    <name>Маршрут</name><styleUrl>#routeStyle</styleUrl>\n");
        sb.append("    <LineString>\n");
        sb.append("      <altitudeMode>relativeToGround</altitudeMode>\n");
        sb.append("      <coordinates>\n");
        for (Waypoint wp : waypoints) {
            sb.append(String.format("        %.7f,%.7f,%.1f\n",
                    wp.longitude, wp.latitude, wp.altitude));
        }
        sb.append("      </coordinates>\n");
        sb.append("    </LineString>\n");
        sb.append("  </Placemark>\n");
        for (int i = 0; i < waypoints.size(); i++) {
            Waypoint wp = waypoints.get(i);
            sb.append("  <Placemark>\n");
            sb.append("    <name>").append(i + 1).append("</name>\n");
            sb.append("    <styleUrl>#wpStyle</styleUrl>\n");
            sb.append("    <Point>\n");
            sb.append("      <altitudeMode>relativeToGround</altitudeMode>\n");
            sb.append(String.format(
                    "      <coordinates>%.7f,%.7f,%.1f</coordinates>\n",
                    wp.longitude, wp.latitude, wp.altitude));
            sb.append("    </Point>\n");
            sb.append("  </Placemark>\n");
        }
        sb.append("</Document>\n</kml>");

        File kmlFile = new File(outputDir, missionName + "_preview.kml");
        try (FileWriter fw = new FileWriter(kmlFile)) {
            fw.write(sb.toString());
        }
        return kmlFile;
    }

    private void addEntry(ZipOutputStream zos, String name,
                          String content) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private String buildWpml(List<Waypoint> waypoints) {
        StringBuilder sb = new StringBuilder();
        int n        = waypoints.size();
        int actionId = 1;   // глобальный счётчик actionId

        // ── Заголовок ─────────────────────────────────────────────────────────
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        // ВАЖНО: правильный неймспейс из реального файла DJI Fly
        sb.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\" xmlns:wpml=\"http://www.uav.com/wpmz/1.0.2\">\n");
        sb.append("  <Document>\n");

        // ── missionConfig ──────────────────────────────────────────────────────
        sb.append("    <wpml:missionConfig>\n");
        sb.append("      <wpml:flyToWaylineMode>safely</wpml:flyToWaylineMode>\n");
        sb.append("      <wpml:finishAction>goHome</wpml:finishAction>\n");
        // goContinue — именно так генерирует DJI Fly
        sb.append("      <wpml:exitOnRCLost>goContinue</wpml:exitOnRCLost>\n");
        sb.append("      <wpml:executeRCLostAction>goBack</wpml:executeRCLostAction>\n");
        sb.append("      <wpml:globalTransitionalSpeed>10</wpml:globalTransitionalSpeed>\n");
        sb.append("      <wpml:droneInfo>\n");
        // 68 = Mavic 4 Pro (из реального файла DJI Fly; у нас было 77 — ошибочно)
        sb.append("        <wpml:droneEnumValue>68</wpml:droneEnumValue>\n");
        sb.append("        <wpml:droneSubEnumValue>0</wpml:droneSubEnumValue>\n");
        sb.append("      </wpml:droneInfo>\n");
        sb.append("    </wpml:missionConfig>\n\n");

        // ── Folder ─────────────────────────────────────────────────────────────
        sb.append("    <Folder>\n");
        sb.append("      <wpml:templateId>0</wpml:templateId>\n");
        sb.append("      <wpml:executeHeightMode>relativeToStartPoint</wpml:executeHeightMode>\n");
        sb.append("      <wpml:waylineId>0</wpml:waylineId>\n");
        sb.append("      <wpml:distance>0</wpml:distance>\n");
        sb.append("      <wpml:duration>0</wpml:duration>\n");
        sb.append(String.format("      <wpml:autoFlightSpeed>%.0f</wpml:autoFlightSpeed>\n\n",
                waypoints.isEmpty() ? 10 : waypoints.get(0).speed));

        // ── Точки маршрута ─────────────────────────────────────────────────────
        for (int i = 0; i < n; i++) {
            Waypoint wp      = waypoints.get(i);
            boolean isFirst  = (i == 0);
            boolean isLast   = (i == n - 1);

            sb.append("      <Placemark>\n");

            // Координаты
            sb.append("        <Point>\n");
            sb.append("          <coordinates>\n");
            sb.append(String.format("            %.7f,%.7f\n", wp.longitude, wp.latitude));
            sb.append("          </coordinates>\n");
            sb.append("        </Point>\n");

            sb.append(String.format("        <wpml:index>%d</wpml:index>\n", i));
            sb.append(String.format("        <wpml:executeHeight>%.0f</wpml:executeHeight>\n",
                    wp.altitude));
            sb.append(String.format("        <wpml:waypointSpeed>%.0f</wpml:waypointSpeed>\n",
                    wp.speed));

            // ── waypointHeadingParam ──────────────────────────────────────────
            sb.append("        <wpml:waypointHeadingParam>\n");
            sb.append("          <wpml:waypointHeadingMode>smoothTransition</wpml:waypointHeadingMode>\n");
            sb.append(String.format(
                    "          <wpml:waypointHeadingAngle>%.0f</wpml:waypointHeadingAngle>\n",
                    wp.yaw));
            sb.append("          <wpml:waypointPoiPoint>0.000000,0.000000,0.000000</wpml:waypointPoiPoint>\n");
            // Enable=1 только на первой и последней точке (паттерн DJI Fly)
            sb.append(String.format(
                    "          <wpml:waypointHeadingAngleEnable>%d</wpml:waypointHeadingAngleEnable>\n",
                    (isFirst || isLast) ? 1 : 0));
            sb.append("          <wpml:waypointHeadingPathMode>followBadArc</wpml:waypointHeadingPathMode>\n");
            sb.append("          <wpml:waypointHeadingPoiIndex>0</wpml:waypointHeadingPoiIndex>\n");
            sb.append("        </wpml:waypointHeadingParam>\n");

            // ── waypointTurnParam ─────────────────────────────────────────────
            // Первая и последняя — Stop, промежуточные — Pass
            sb.append("        <wpml:waypointTurnParam>\n");
            sb.append(String.format(
                    "          <wpml:waypointTurnMode>%s</wpml:waypointTurnMode>\n",
                    (isFirst || isLast)
                            ? "toPointAndStopWithContinuityCurvature"
                            : "toPointAndPassWithContinuityCurvature"));
            sb.append("          <wpml:waypointTurnDampingDist>0</wpml:waypointTurnDampingDist>\n");
            sb.append("        </wpml:waypointTurnParam>\n");

            sb.append("        <wpml:useStraightLine>0</wpml:useStraightLine>\n");

            // ── actionGroup 1: takePhoto (+ начальный gimbalRotate на точке 0) ─
            sb.append("        <wpml:actionGroup>\n");
            sb.append("          <wpml:actionGroupId>1</wpml:actionGroupId>\n");
            sb.append(String.format(
                    "          <wpml:actionGroupStartIndex>%d</wpml:actionGroupStartIndex>\n", i));
            sb.append(String.format(
                    "          <wpml:actionGroupEndIndex>%d</wpml:actionGroupEndIndex>\n", i));
            sb.append("          <wpml:actionGroupMode>parallel</wpml:actionGroupMode>\n");
            sb.append("          <wpml:actionTrigger>\n");
            sb.append("            <wpml:actionTriggerType>reachPoint</wpml:actionTriggerType>\n");
            sb.append("          </wpml:actionTrigger>\n");

            // takePhoto
            sb.append("          <wpml:action>\n");
            sb.append(String.format("            <wpml:actionId>%d</wpml:actionId>\n", actionId++));
            sb.append("            <wpml:actionActuatorFunc>takePhoto</wpml:actionActuatorFunc>\n");
            sb.append("            <wpml:actionActuatorFuncParam>\n");
            sb.append("              <wpml:payloadPositionIndex>0</wpml:payloadPositionIndex>\n");
            sb.append("              <wpml:useGlobalPayloadLensIndex>0</wpml:useGlobalPayloadLensIndex>\n");
            sb.append("            </wpml:actionActuatorFuncParam>\n");
            sb.append("          </wpml:action>\n");

            // gimbalRotate — только на первой точке для установки начального угла
            if (isFirst) {
                sb.append("          <wpml:action>\n");
                sb.append(String.format(
                        "            <wpml:actionId>%d</wpml:actionId>\n", actionId++));
                sb.append("            <wpml:actionActuatorFunc>gimbalRotate</wpml:actionActuatorFunc>\n");
                sb.append("            <wpml:actionActuatorFuncParam>\n");
                sb.append("              <wpml:gimbalHeadingYawBase>aircraft</wpml:gimbalHeadingYawBase>\n");
                sb.append("              <wpml:gimbalRotateMode>absoluteAngle</wpml:gimbalRotateMode>\n");
                sb.append("              <wpml:gimbalPitchRotateEnable>1</wpml:gimbalPitchRotateEnable>\n");
                sb.append("              <wpml:gimbalPitchRotateAngle>-90</wpml:gimbalPitchRotateAngle>\n");
                sb.append("              <wpml:gimbalRollRotateEnable>1</wpml:gimbalRollRotateEnable>\n");
                sb.append("              <wpml:gimbalRollRotateAngle>0</wpml:gimbalRollRotateAngle>\n");
                sb.append("              <wpml:gimbalYawRotateEnable>0</wpml:gimbalYawRotateEnable>\n");
                sb.append("              <wpml:gimbalYawRotateAngle>0</wpml:gimbalYawRotateAngle>\n");
                sb.append("              <wpml:gimbalRotateTimeEnable>0</wpml:gimbalRotateTimeEnable>\n");
                sb.append("              <wpml:gimbalRotateTime>0</wpml:gimbalRotateTime>\n");
                sb.append("              <wpml:payloadPositionIndex>0</wpml:payloadPositionIndex>\n");
                sb.append("            </wpml:actionActuatorFuncParam>\n");
                sb.append("          </wpml:action>\n");
            }

            sb.append("        </wpml:actionGroup>\n");

            // ── actionGroup 2: gimbalEvenlyRotate (не на последней точке) ──────
            if (!isLast) {
                sb.append("        <wpml:actionGroup>\n");
                sb.append("          <wpml:actionGroupId>2</wpml:actionGroupId>\n");
                sb.append(String.format(
                        "          <wpml:actionGroupStartIndex>%d</wpml:actionGroupStartIndex>\n", i));
                sb.append(String.format(
                        "          <wpml:actionGroupEndIndex>%d</wpml:actionGroupEndIndex>\n", i + 1));
                sb.append("          <wpml:actionGroupMode>parallel</wpml:actionGroupMode>\n");
                sb.append("          <wpml:actionTrigger>\n");
                sb.append("            <wpml:actionTriggerType>reachPoint</wpml:actionTriggerType>\n");
                sb.append("          </wpml:actionTrigger>\n");
                sb.append("          <wpml:action>\n");
                sb.append(String.format(
                        "            <wpml:actionId>%d</wpml:actionId>\n", actionId++));
                sb.append("            <wpml:actionActuatorFunc>gimbalEvenlyRotate</wpml:actionActuatorFunc>\n");
                sb.append("            <wpml:actionActuatorFuncParam>\n");
                sb.append("              <wpml:gimbalPitchRotateAngle>-90</wpml:gimbalPitchRotateAngle>\n");
                sb.append("              <wpml:gimbalRollRotateAngle>0</wpml:gimbalRollRotateAngle>\n");
                sb.append("              <wpml:payloadPositionIndex>0</wpml:payloadPositionIndex>\n");
                sb.append("            </wpml:actionActuatorFuncParam>\n");
                sb.append("          </wpml:action>\n");
                sb.append("        </wpml:actionGroup>\n");
            }

            // ── waypointGimbalHeadingParam ────────────────────────────────────
            sb.append("        <wpml:waypointGimbalHeadingParam>\n");
            sb.append("          <wpml:waypointGimbalPitchAngle>0</wpml:waypointGimbalPitchAngle>\n");
            sb.append("          <wpml:waypointGimbalYawAngle>0</wpml:waypointGimbalYawAngle>\n");
            sb.append("        </wpml:waypointGimbalHeadingParam>\n");

            sb.append("      </Placemark>\n\n");
        }

        sb.append("    </Folder>\n");
        sb.append("  </Document>\n");
        sb.append("</kml>");
        return sb.toString();
    }

    private String buildTemplate() {
        long now = System.currentTimeMillis();
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<kml xmlns=\"http://www.opengis.net/kml/2.2\" xmlns:wpml=\"http://www.uav.com/wpmz/1.0.2\">\n"
                + "  <Document>\n"
                + "    <wpml:author>fly</wpml:author>\n"
                + "    <wpml:createTime>" + now + "</wpml:createTime>\n"
                + "    <wpml:updateTime>" + now + "</wpml:updateTime>\n"
                + "    <wpml:missionConfig>\n"
                + "      <wpml:flyToWaylineMode>safely</wpml:flyToWaylineMode>\n"
                + "      <wpml:finishAction>goHome</wpml:finishAction>\n"
                + "      <wpml:exitOnRCLost>goContinue</wpml:exitOnRCLost>\n"
                + "      <wpml:executeRCLostAction>goBack</wpml:executeRCLostAction>\n"
                + "      <wpml:globalTransitionalSpeed>10</wpml:globalTransitionalSpeed>\n"
                + "      <wpml:droneInfo>\n"
                + "        <wpml:droneEnumValue>68</wpml:droneEnumValue>\n"
                + "        <wpml:droneSubEnumValue>0</wpml:droneSubEnumValue>\n"
                + "      </wpml:droneInfo>\n"
                + "    </wpml:missionConfig>\n"
                + "  </Document>\n"
                + "</kml>";
    }
}