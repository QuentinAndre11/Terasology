// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.engine.rendering;

import org.lwjgl.opengl.GL11;
import org.terasology.gestalt.module.sandbox.API;
import org.terasology.engine.logic.players.LocalPlayer;
import org.terasology.engine.math.AABB;
import org.terasology.math.geom.Vector3f;
import org.terasology.math.geom.Vector4f;
import org.terasology.engine.registry.CoreRegistry;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_LINE_LOOP;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glCallList;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glDeleteLists;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glEndList;
import static org.lwjgl.opengl.GL11.glGenLists;
import static org.lwjgl.opengl.GL11.glNewList;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glScalef;
import static org.lwjgl.opengl.GL11.glTranslated;
import static org.lwjgl.opengl.GL11.glVertex3f;

/**
 * Renderer for an AABB.
 */
@API
public class AABBRenderer implements BlockOverlayRenderer {
    private int displayListWire = -1;
    private int displayListSolid = -1;
    private Vector4f solidColor = new Vector4f(1f, 1f, 1f, 1f);

    private AABB aabb;

    public AABBRenderer(AABB aabb) {
        this.aabb = aabb;
    }

    @Override
    public void setAABB(AABB from) {
        if (from != null && !from.equals(this.aabb)) {
            this.aabb = from;
            dispose();
        }
    }

    public void dispose() {
        if (displayListWire != -1) {
            glDeleteLists(displayListWire, 1);
            displayListWire = -1;
        }
        if (displayListSolid != -1) {
            glDeleteLists(displayListSolid, 1);
            displayListSolid = -1;
        }
    }

    public void setSolidColor(Vector4f color) {
        solidColor = color;
    }

    /**
     * Renders this AABB.
     * <br><br>
     */
    @Override
    public void render() {
        CoreRegistry.get(ShaderManager.class).enableDefault();

        glPushMatrix();
        Vector3f cameraPosition = CoreRegistry.get(LocalPlayer.class).getViewPosition();
        glTranslated(aabb.getCenter().x - cameraPosition.x, -cameraPosition.y, aabb.getCenter().z - cameraPosition.z);

        renderLocally();

        glPopMatrix();
    }

    public void renderSolid() {
        CoreRegistry.get(ShaderManager.class).enableDefault();

        glPushMatrix();
        Vector3f cameraPosition = CoreRegistry.get(LocalPlayer.class).getViewPosition();
        glTranslated(aabb.getCenter().x - cameraPosition.x, -cameraPosition.y, aabb.getCenter().z - cameraPosition.z);

        renderSolidLocally();

        glPopMatrix();
    }

    /**
     * Maintained for API compatibility.
     */
    public void renderLocally(float ignored) {
        renderLocally();
    }

    public void renderLocally() {
        CoreRegistry.get(ShaderManager.class).enableDefault();

        if (displayListWire == -1) {
            generateDisplayListWire();
        }

        glPushMatrix();
        glTranslated(0f, aabb.getCenter().y, 0f);

        glCallList(displayListWire);

        glPopMatrix();
    }

    public void renderSolidLocally() {
        CoreRegistry.get(ShaderManager.class).enableDefault();

        if (displayListSolid == -1) {
            generateDisplayListSolid();
        }
        glEnable(GL_BLEND);
        glPushMatrix();

        glTranslated(0f, aabb.getCenter().y, 0f);
        glScalef(1.5f, 1.5f, 1.5f);

        glCallList(displayListSolid);

        glPopMatrix();
        glDisable(GL_BLEND);
    }

    private void generateDisplayListSolid() {
        displayListSolid = glGenLists(1);

        glNewList(displayListSolid, GL11.GL_COMPILE);
        glBegin(GL_QUADS);
        glColor4f(solidColor.x, solidColor.y, solidColor.z, solidColor.w);

        Vector3f dimensions = aabb.getExtents();

        GL11.glVertex3f(-dimensions.x, dimensions.y, dimensions.z);
        GL11.glVertex3f(dimensions.x, dimensions.y, dimensions.z);
        GL11.glVertex3f(dimensions.x, dimensions.y, -dimensions.z);
        GL11.glVertex3f(-dimensions.x, dimensions.y, -dimensions.z);

        GL11.glVertex3f(-dimensions.x, -dimensions.y, -dimensions.z);
        GL11.glVertex3f(-dimensions.x, -dimensions.y, dimensions.z);
        GL11.glVertex3f(-dimensions.x, dimensions.y, dimensions.z);
        GL11.glVertex3f(-dimensions.x, dimensions.y, -dimensions.z);

        GL11.glVertex3f(-dimensions.x, -dimensions.y, dimensions.z);
        GL11.glVertex3f(dimensions.x, -dimensions.y, dimensions.z);
        GL11.glVertex3f(dimensions.x, dimensions.y, dimensions.z);
        GL11.glVertex3f(-dimensions.x, dimensions.y, dimensions.z);

        GL11.glVertex3f(dimensions.x, dimensions.y, -dimensions.z);
        GL11.glVertex3f(dimensions.x, dimensions.y, dimensions.z);
        GL11.glVertex3f(dimensions.x, -dimensions.y, dimensions.z);
        GL11.glVertex3f(dimensions.x, -dimensions.y, -dimensions.z);

        GL11.glVertex3f(-dimensions.x, dimensions.y, -dimensions.z);
        GL11.glVertex3f(dimensions.x, dimensions.y, -dimensions.z);
        GL11.glVertex3f(dimensions.x, -dimensions.y, -dimensions.z);
        GL11.glVertex3f(-dimensions.x, -dimensions.y, -dimensions.z);

        GL11.glVertex3f(-dimensions.x, -dimensions.y, -dimensions.z);
        GL11.glVertex3f(dimensions.x, -dimensions.y, -dimensions.z);
        GL11.glVertex3f(dimensions.x, -dimensions.y, dimensions.z);
        GL11.glVertex3f(-dimensions.x, -dimensions.y, dimensions.z);
        glEnd();
        glEndList();

    }

    private void generateDisplayListWire() {
        float offset = 0.001f;

        displayListWire = glGenLists(1);

        glNewList(displayListWire, GL11.GL_COMPILE);
        glColor4f(0.0f, 0.0f, 0.0f, 1.0f);

        Vector3f dimensions = aabb.getExtents();

        // FRONT
        glBegin(GL_LINE_LOOP);
        glVertex3f(-dimensions.x - offset, -dimensions.y - offset, -dimensions.z - offset);
        glVertex3f(+dimensions.x + offset, -dimensions.y - offset, -dimensions.z - offset);
        glVertex3f(+dimensions.x + offset, +dimensions.y + offset, -dimensions.z - offset);
        glVertex3f(-dimensions.x - offset, +dimensions.y + offset, -dimensions.z - offset);
        glEnd();

        // BACK
        glBegin(GL_LINE_LOOP);
        glVertex3f(-dimensions.x - offset, -dimensions.y - offset, +dimensions.z + offset);
        glVertex3f(+dimensions.x + offset, -dimensions.y - offset, +dimensions.z + offset);
        glVertex3f(+dimensions.x + offset, +dimensions.y + offset, +dimensions.z + offset);
        glVertex3f(-dimensions.x - offset, +dimensions.y + offset, +dimensions.z + offset);
        glEnd();

        // TOP
        glBegin(GL_LINE_LOOP);
        glVertex3f(-dimensions.x - offset, -dimensions.y - offset, -dimensions.z - offset);
        glVertex3f(+dimensions.x + offset, -dimensions.y - offset, -dimensions.z - offset);
        glVertex3f(+dimensions.x + offset, -dimensions.y - offset, +dimensions.z + offset);
        glVertex3f(-dimensions.x - offset, -dimensions.y - offset, +dimensions.z + offset);
        glEnd();

        // BOTTOM
        glBegin(GL_LINE_LOOP);
        glVertex3f(-dimensions.x - offset, +dimensions.y + offset, -dimensions.z - offset);
        glVertex3f(+dimensions.x + offset, +dimensions.y + offset, -dimensions.z - offset);
        glVertex3f(+dimensions.x + offset, +dimensions.y + offset, +dimensions.z + offset);
        glVertex3f(-dimensions.x - offset, +dimensions.y + offset, +dimensions.z + offset);
        glEnd();

        // LEFT
        glBegin(GL_LINE_LOOP);
        glVertex3f(-dimensions.x - offset, -dimensions.y - offset, -dimensions.z - offset);
        glVertex3f(-dimensions.x - offset, -dimensions.y - offset, +dimensions.z + offset);
        glVertex3f(-dimensions.x - offset, +dimensions.y + offset, +dimensions.z + offset);
        glVertex3f(-dimensions.x - offset, +dimensions.y + offset, -dimensions.z - offset);
        glEnd();

        // RIGHT
        glBegin(GL_LINE_LOOP);
        glVertex3f(+dimensions.x + offset, -dimensions.y - offset, -dimensions.z - offset);
        glVertex3f(+dimensions.x + offset, -dimensions.y - offset, +dimensions.z + offset);
        glVertex3f(+dimensions.x + offset, +dimensions.y + offset, +dimensions.z + offset);
        glVertex3f(+dimensions.x + offset, +dimensions.y + offset, -dimensions.z - offset);
        glEnd();
        glEndList();
    }

    public AABB getAABB() {
        return aabb;
    }
}