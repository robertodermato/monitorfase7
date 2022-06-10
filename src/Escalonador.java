import java.util.LinkedList;

public class Escalonador {

    private int posicaoEscalonador;
    private  GerenciadorProcessos gp;

    public Escalonador(GerenciadorProcessos gp){
        posicaoEscalonador = 0;
        this.gp = gp;
    }

    public void runEscalonador(int programCounter, int [] registradores, Sistema.Word instructionRegister, Interrupts interrupt, int[] paginasAlocadas){
        gp.running = gp.prontos.get(posicaoEscalonador);

        // debug
            /*
            System.out.println("Processo em execução e que será parado é id: " + running.getId() + " e seu Pc está em: " + running.getProgramCounter());
            System.out.println("Sua posição no escalonador é " + posicaoEscalonador);
            int [] paginasAlocadasDoProcessoAtual = running.getPaginasAlocadas();
            System.out.print("Suas páginas alocadas são " );
            for (int i=0; i<paginasAlocadasDoProcessoAtual.length; i++){
                System.out.println(paginasAlocadasDoProcessoAtual[i] + " ");
            }
            System.out.println(" ");
             */

        // seta as variáveis do processo atual com estado atual da CPU
        gp.running.setContext(programCounter, registradores, instructionRegister, interrupt);

        // para poder ciclar a posição do escalonador
        posicaoEscalonador =  (posicaoEscalonador + 1) % gp.prontos.size();

        gp.running = gp.prontos.get(posicaoEscalonador);

        // debug
            /*
            System.out.println("Processo para iniciar execução é id: " + running.getId() + " e seu Pc está em: " + running.getProgramCounter());
            System.out.println("Sua posição no escalonador é " + posicaoEscalonador);
            paginasAlocadasDoProcessoAtual = running.getPaginasAlocadas();
            System.out.print("Suas páginas alocadas são " );
            for (int i=0; i<paginasAlocadasDoProcessoAtual.length; i++){
                System.out.println(paginasAlocadasDoProcessoAtual[i] + " ");
            }
            System.out.println(" ");
             */

        gp.setCPUforRunningProcess();
    }

}
