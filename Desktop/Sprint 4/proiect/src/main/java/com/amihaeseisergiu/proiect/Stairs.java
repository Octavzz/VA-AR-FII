/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.amihaeseisergiu.proiect;

/**
 *
 * @author Sergiu
 */
public class Stairs extends ExtendedRectangle {
    
    public Stairs(Point p) {
        super(p);
        this.type = "connector";
        this.shapeType = "Stairs";
    }
    
}
