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
        String wpml = buildWpml(waypoints);
        String template = buildTemplate(missionName);

        File kmzFile = new File(outputDir, missionName + ".kmz");
        try (ZipOutputStream zos = new ZipOutputStream(
                new FileOutputStream(kmzFile))) {
            addEntry(zos, "wpmz/waylines.wpml", wpml);
            addEntry(zos, "wpmz/template.kml", template);
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
        sb.append("    <LineStyle>\n");
        sb.append("      <color>ffff5722</color>\n");
        sb.append("      <width>3</width>\n");
        sb.append("    </LineStyle>\n");
        sb.append("  </Style>\n");

        sb.append("  <Style id=\"waypointStyle\">\n");
        sb.append("    <IconStyle>\n");
        sb.append("      <color>ff2196F3</color>\n");
        sb.append("      <scale>0.6</scale>\n");
        sb.append("    </IconStyle>\n");
        sb.append("  </Style>\n");

        sb.append("  <Placemark>\n");
        sb.append("    <name>Маршрут</name>\n");
        sb.append("    <styleUrl>#routeStyle</styleUrl>\n");
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
            sb.append("    <styleUrl>#waypointStyle</styleUrl>\n");
            sb.append("    <Point>\n");
            sb.append("      <altitudeMode>relativeToGround</altitudeMode>\n");
            sb.append(String.format(
                    "      <coordinates>%.7f,%.7f,%.1f</coordinates>\n",
                    wp.longitude, wp.latitude, wp.altitude));
            sb.append("    </Point>\n");
            sb.append("  </Placemark>\n");
        }

        sb.append("</Document>\n");
        sb.append("</kml>");

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
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\" ");
        sb.append("xmlns:wpml=\"http://www.dji.com/wpmz/1.0.2\">\n");
        sb.append("<Document>\n");

        sb.append("  <wpml:missionConfig>\n");
        sb.append("    <wpml:flyToWaylineMode>safely</wpml:flyToWaylineMode>\n");
        sb.append("    <wpml:finishAction>goHome</wpml:finishAction>\n");
        sb.append("    <wpml:exitOnRCLost>executeLostAction</wpml:exitOnRCLost>\n");
        sb.append("    <wpml:executeRCLostAction>goBack</wpml:executeRCLostAction>\n");
        sb.append("    <wpml:takeOffSecurityHeight>20</wpml:takeOffSecurityHeight>\n");
        sb.append("    <wpml:globalTransitionalSpeed>10</wpml:globalTransitionalSpeed>\n");
        sb.append("    <wpml:droneInfo>\n");
        sb.append("      <wpml:droneEnumValue>77</wpml:droneEnumValue>\n");
        sb.append("      <wpml:droneSubEnumValue>0</wpml:droneSubEnumValue>\n");
        sb.append("    </wpml:droneInfo>\n");
        sb.append("  </wpml:missionConfig>\n\n");

        sb.append("  <Folder>\n");
        sb.append("    <wpml:templateId>0</wpml:templateId>\n");
        sb.append("    <wpml:executeHeightMode>relativeToStartPoint</wpml:executeHeightMode>\n");
        sb.append("    <wpml:waylineId>0</wpml:waylineId>\n");
        sb.append("    <wpml:autoFlightSpeed>10</wpml:autoFlightSpeed>\n\n");

        for (int i = 0; i < waypoints.size(); i++) {
            Waypoint wp = waypoints.get(i);
            sb.append("    <Placemark>\n");
            sb.append("      <Point>\n");
            sb.append(String.format(
                    "        <coordinates>%.7f,%.7f,0</coordinates>\n",
                    wp.longitude, wp.latitude));
            sb.append("      </Point>\n");
            sb.append(String.format(
                    "      <wpml:index>%d</wpml:index>\n", i));
            sb.append(String.format(
                    "      <wpml:executeHeight>%.1f</wpml:executeHeight>\n",
                    wp.altitude));
            sb.append(String.format(
                    "      <wpml:waypointSpeed>%.1f</wpml:waypointSpeed>\n",
                    wp.speed));
            sb.append("      <wpml:waypointHeadingParam>\n");
            sb.append("        <wpml:waypointHeadingMode>smoothTransition</wpml:waypointHeadingMode>\n");
            sb.append(String.format(
                    "        <wpml:waypointHeadingAngle>%.0f</wpml:waypointHeadingAngle>\n",
                    wp.yaw));
            sb.append("        <wpml:waypointPoiPoint>0.000000,0.000000,0.000000</wpml:waypointPoiPoint>\n");
            sb.append("      </wpml:waypointHeadingParam>\n");
            sb.append("      <wpml:waypointTurnParam>\n");
            sb.append("        <wpml:waypointTurnMode>toPointAndPassWithContinuityCurvature</wpml:waypointTurnMode>\n");
            sb.append("        <wpml:waypointTurnDampingDist>0</wpml:waypointTurnDampingDist>\n");
            sb.append("      </wpml:waypointTurnParam>\n");

            // Действия в точке
            sb.append("      <wpml:actionGroup>\n");
            sb.append(String.format(
                    "        <wpml:actionGroupId>%d</wpml:actionGroupId>\n", i));
            sb.append(String.format(
                    "        <wpml:actionGroupStartIndex>%d</wpml:actionGroupStartIndex>\n", i));
            sb.append(String.format(
                    "        <wpml:actionGroupEndIndex>%d</wpml:actionGroupEndIndex>\n", i));
            sb.append("        <wpml:actionGroupMode>sequence</wpml:actionGroupMode>\n");
            sb.append("        <wpml:actionTrigger>\n");
            sb.append("          <wpml:actionTriggerType>reachPoint</wpml:actionTriggerType>\n");
            sb.append("        </wpml:actionTrigger>\n");

            // Действие 0: повернуть дрон
            sb.append("        <wpml:action>\n");
            sb.append("          <wpml:actionId>0</wpml:actionId>\n");
            sb.append("          <wpml:actionActuatorFunc>rotateYaw</wpml:actionActuatorFunc>\n");
            sb.append("          <wpml:actionActuatorFuncParam>\n");
            sb.append(String.format(
                    "            <wpml:aircraftHeading>%.0f</wpml:aircraftHeading>\n",
                    wp.yaw));
            sb.append("            <wpml:aircraftPathMode>clockwise</wpml:aircraftPathMode>\n");
            sb.append("          </wpml:actionActuatorFuncParam>\n");
            sb.append("        </wpml:action>\n");

            // Действие 1: наклонить камеру вертикально вниз
            sb.append("        <wpml:action>\n");
            sb.append("          <wpml:actionId>1</wpml:actionId>\n");
            sb.append("          <wpml:actionActuatorFunc>gimbalRotate</wpml:actionActuatorFunc>\n");
            sb.append("          <wpml:actionActuatorFuncParam>\n");
            sb.append("            <wpml:gimbalHeadingYawBase>aircraft</wpml:gimbalHeadingYawBase>\n");
            sb.append("            <wpml:gimbalRotateMode>absoluteAngle</wpml:gimbalRotateMode>\n");
            sb.append("            <wpml:gimbalPitchRotateEnable>1</wpml:gimbalPitchRotateEnable>\n");
            sb.append("            <wpml:gimbalPitchRotateAngle>-90</wpml:gimbalPitchRotateAngle>\n");
            sb.append("            <wpml:gimbalRollRotateEnable>0</wpml:gimbalRollRotateEnable>\n");
            sb.append("            <wpml:gimbalRollRotateAngle>0</wpml:gimbalRollRotateAngle>\n");
            sb.append("            <wpml:gimbalYawRotateEnable>0</wpml:gimbalYawRotateEnable>\n");
            sb.append("            <wpml:gimbalYawRotateAngle>0</wpml:gimbalYawRotateAngle>\n");
            sb.append("            <wpml:gimbalRotateTimeEnable>0</wpml:gimbalRotateTimeEnable>\n");
            sb.append("            <wpml:gimbalRotateTime>0</wpml:gimbalRotateTime>\n");
            sb.append("            <wpml:payloadPositionIndex>0</wpml:payloadPositionIndex>\n");
            sb.append("          </wpml:actionActuatorFuncParam>\n");
            sb.append("        </wpml:action>\n");

            // Действие 2: сделать фото
            sb.append("        <wpml:action>\n");
            sb.append("          <wpml:actionId>2</wpml:actionId>\n");
            sb.append("          <wpml:actionActuatorFunc>takePhoto</wpml:actionActuatorFunc>\n");
            sb.append("          <wpml:actionActuatorFuncParam>\n");
            sb.append("            <wpml:payloadPositionIndex>0</wpml:payloadPositionIndex>\n");
            sb.append("          </wpml:actionActuatorFuncParam>\n");
            sb.append("        </wpml:action>\n");

            sb.append("      </wpml:actionGroup>\n");
            sb.append("    </Placemark>\n\n");
        }

        sb.append("  </Folder>\n");
        sb.append("</Document>\n");
        sb.append("</kml>");
        return sb.toString();
    }

    private String buildTemplate(String missionName) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<kml xmlns=\"http://www.opengis.net/kml/2.2\" " +
                "xmlns:wpml=\"http://www.dji.com/wpmz/1.0.2\">\n" +
                "<Document>\n" +
                "  <wpml:author>FlyBaiTau</wpml:author>\n" +
                "  <wpml:createTime>" + System.currentTimeMillis() + "</wpml:createTime>\n" +
                "  <wpml:updateTime>" + System.currentTimeMillis() + "</wpml:updateTime>\n" +
                "  <wpml:missionConfig>\n" +
                "    <wpml:flyToWaylineMode>safely</wpml:flyToWaylineMode>\n" +
                "    <wpml:finishAction>goHome</wpml:finishAction>\n" +
                "    <wpml:exitOnRCLost>executeLostAction</wpml:exitOnRCLost>\n" +
                "    <wpml:executeRCLostAction>goBack</wpml:executeRCLostAction>\n" +
                "    <wpml:takeOffSecurityHeight>20</wpml:takeOffSecurityHeight>\n" +
                "    <wpml:globalTransitionalSpeed>10</wpml:globalTransitionalSpeed>\n" +
                "    <wpml:droneInfo>\n" +
                "      <wpml:droneEnumValue>77</wpml:droneEnumValue>\n" +
                "      <wpml:droneSubEnumValue>0</wpml:droneSubEnumValue>\n" +
                "    </wpml:droneInfo>\n" +
                "  </wpml:missionConfig>\n" +
                "</Document>\n" +
                "</kml>";
    }
}