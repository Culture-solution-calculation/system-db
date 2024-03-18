package org.main.culturesolutioncalculation.service.calculator;

public class FinalCal {
        String compound_kr;
        double mass;

        public FinalCal(String compound_kr, double mass) {
            this.compound_kr = compound_kr;
            this.mass = mass;
        }

        public String getCompound_kr() {
            return compound_kr;
        }

        public double getMass() {
            return mass;
        }

        public void setMass(double mass) {
            this.mass = mass;
        }

}
