public class PCB {

    public int id;
    public int programCounter;
    public int[] paginasAlocadas;
    public int[] registradores;
    public Sistema.Word instructionRegister;
    public Sistema.Interrupts interrupt;

    public PCB(int id, int[]paginasAlocadas, int pc, int [] reg, Sistema.Word ir, Sistema.Interrupts interrupt) {
        this.id= id;
        this.paginasAlocadas = paginasAlocadas;
        this.programCounter = pc;
        this.registradores = new int[reg.length];
        this.instructionRegister = ir;
        this.interrupt = interrupt;
    }

    public int[] getPaginasAlocadas(){
        return this.paginasAlocadas;
    }

    public int getId(){
        return this.id;
    }

    public int getProgramCounter(){
        return programCounter;
    }

    public int[] getRegistradores(){
        return registradores;
    }

    public Sistema.Word getInstructionRegister(){
        return instructionRegister;
    }

    public Sistema.Interrupts getInterrupt(){
        return interrupt;
    }

    public void setContext (int programCounter, int[] registradores, Sistema.Word instructionRegister, Sistema.Interrupts interrupt){
        this.programCounter = programCounter;
        this.registradores =registradores;
        this.instructionRegister = instructionRegister;
        this.interrupt = interrupt;
    }

    public String toString(){
        String paginasAlocadasString = "[";
        for (int i=0; i<paginasAlocadas.length; i++){
            paginasAlocadasString = paginasAlocadasString + " " + paginasAlocadas[i];
        }
        paginasAlocadasString = paginasAlocadasString + "]";

        String processo = "Process id: " + id + ", Program counter: " + programCounter + ", Páginas alocadas: " + paginasAlocadasString;
        return processo;
    }
}