package org.main.culturesolutioncalculation.service.calculator;

public class FinalCal {
    String solution;
    double mass;

    public FinalCal(String solution, double mass) {
        this.solution = solution;
        this.mass = mass;
    }

    public String getSolution() {
        return solution;
    }

    public void setSolution(String solution) {
        this.solution = solution;
    }

    public double getMass() {
            return mass;
        }

    public void setMass(double mass) {
            this.mass = mass;
        }

}
