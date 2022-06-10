import java.util.LinkedList;

public class GerenciadorProcessos {
    private GerenciadorMemoria gm;
    private Sistema.Word[] memory;

    public LinkedList<PCB> prontos;
    private LinkedList<PCB> bloqueados;
    public PCB running;

    private int process_id;
    public int posicaoEscalonador;
    public int tamPagina;

    public Sistema.CPU cpu;

    public GerenciadorProcessos(GerenciadorMemoria gm, Sistema.Word[] memory, Sistema.CPU cpu) {
        process_id=0;

        this.cpu = cpu;

        this.gm = gm;
        this.memory = memory;

        this.prontos = new LinkedList<>();
        this.bloqueados = new LinkedList<>();

        this.posicaoEscalonador = 0;
        this.tamPagina = gm.getTamPagina();
    }

    public LinkedList<PCB> getProntos() {
        return prontos;
    }

    public PCB getRunning(){
        return running;
    }

    public void setRunning(PCB processo){
        running = processo;
    }


    // retorna -1 se não achar o processo ou um array com as páginas alocadas do processo
    public int[] getPaginasAlocadas (int process_id){
        int [] paginasAlocadas = new int[1];
        boolean achou = false;
        for (int i = 0; i < prontos.size(); i++) {
            if (prontos.get(i).id==process_id){
                paginasAlocadas = prontos.get(i).paginasAlocadas;
                achou = true;
            }
        }

        if (achou==false){
            paginasAlocadas= new int[1];
            paginasAlocadas[0]=-1;
        }

        return paginasAlocadas;
    }

    public PCB getProcesso (int process_id){
        PCB processo = null;
        for (int i = 0; i < prontos.size(); i++) {
            if (prontos.get(i).id==process_id){
                processo = prontos.get(i);
            }
        }
        return processo;
    }

    // Se o processo não foi criado por falta de memória, retorna -1, caso contrário retorna o número do processo criado
    public int criaProcesso(Sistema.Word[] programa){
        int[] paginasAlocadas = gm.aloca(programa);

        // Se o processo não foi criado por falta de memória, retorna -1
        if (paginasAlocadas[0]==-1){
            return -1;
        }

        System.out.println(" ");
        System.out.println("------ Processo " + process_id + " criado -------------");

        int indicePrograma = 0;
        for (int i=0; i<paginasAlocadas.length; i++) {
            for (int j = tamPagina * paginasAlocadas[i]; j < tamPagina * (paginasAlocadas[i] + 1); j++) {
                if (indicePrograma >= programa.length) break;
                memory[j].opc = programa[indicePrograma].opc;
                memory[j].r1 = programa[indicePrograma].r1;
                memory[j].r2 = programa[indicePrograma].r2;
                memory[j].p = programa[indicePrograma].p;
                indicePrograma++;
            }
        }

        // referências problemáticas aqui??? vm.cpu...
        PCB processo = new PCB(process_id, paginasAlocadas, 0, new int[10], new Sistema.Word(Sistema.Opcode.___,-1,-1,-1), Sistema.Interrupts.INT_NONE);
        //PCB processo = new PCB(process_id, paginasAlocadas, vm.cpu.getPc(), vm.cpu.getReg(), vm.cpu.getIr(), vm.cpu.getInterrupts());
        prontos.add(processo);

        process_id++;

        //debug
            /*
            System.out.println("Páginas alocadas durante a criação do processo pelo GP: ");
            for (int i=0; i<paginasAlocadas.length; i++){
                System.out.println(paginasAlocadas[i] + " ");
            }
             */

        return process_id-1;
    }


    public void desalocaProcesso(PCB processo){
        int [] paginasAlocadas = processo.getPaginasAlocadas();

        System.out.println(" ");
        System.out.println("------ Estado da memória do programa que será desalocado ---------");
        System.out.println("------ Para poder observar o resultado da execução ---------------");
        for(int i = 0; i < paginasAlocadas.length; i ++) {
            gm.dumpPagina(memory, paginasAlocadas[i]);
        }

        // reseta as posições da memória
        for(int i = 0; i < paginasAlocadas.length; i ++) {
            for (int j = tamPagina * paginasAlocadas[i]; j < tamPagina * (paginasAlocadas[i] + 1); j++) {
                memory[j].opc = Sistema.Opcode.___;
                memory[j].r1 = -1;
                memory[j].r2 = -1;
                memory[j].p = -1;
            }
        }
        gm.desaloca(processo);
        prontos.remove(processo);
        System.out.println(" ");
        System.out.println("-------------- Removido o processo " + processo.getId());
    }

    public void setCPUforRunningProcess(){
        // pega o contexto do processo que ira rodar agora
        int programCounterDoRunning = running.getProgramCounter();
        int [] paginasAlocadasDoRunning = running.getPaginasAlocadas();
        int [] registradoresdoRunning = running.getRegistradores();
        Sistema.Word instructionRegisterDoRunning = running.getInstructionRegister();
        Sistema.Interrupts interruptsDoRunning = running.getInterrupt();

        cpu.setContext(programCounterDoRunning, paginasAlocadasDoRunning, registradoresdoRunning, instructionRegisterDoRunning, interruptsDoRunning);
    }

    public void listaProcessos(){
        System.out.println("-------------Processo em execução:");
        if (this.running==null) System.out.println("Nenhum");
        else System.out.println(this.running);

        System.out.println("-------------Processos na lista de prontos");
        if (this.prontos.size()==0) System.out.println("Nenhum");
        else {
            for (PCB processo : this.prontos) {
                System.out.println(processo);
            }
        }

        System.out.println("-------------Processos na lista de bloqueados");
        if (this.bloqueados.size()==0) System.out.println("Nenhum");
        else {
            for (PCB processo : this.bloqueados) {
                System.out.println(processo);
            }
        }
    }

    public void runEscalonador(int programCounter, int [] registradores, Sistema.Word instructionRegister, Sistema.Interrupts interrupt, int[] paginasAlocadas){
        running = prontos.get(posicaoEscalonador);

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
        running.setContext(programCounter, registradores, instructionRegister, interrupt);

        // para poder ciclar a posição do escalonador
        posicaoEscalonador =  (posicaoEscalonador + 1) % prontos.size();

        running = prontos.get(posicaoEscalonador);

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

        setCPUforRunningProcess();
    }
}