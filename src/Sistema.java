// PUCRS - Escola Politécnica - Sistemas Operacionais
// Prof. Fernando Dotti
// Código fornecido como parte da solução do projeto de Sistemas Operacionais
//
// Fase 1 - máquina virtual (vide enunciado correspondente)
//


import java.util.LinkedList;


public class Sistema {


    // -------------------------------------------------------------------------------------------------------
    // --------------------- H A R D W A R E - definicoes de HW ----------------------------------------------

    // -------------------------------------------------------------------------------------------------------
    // --------------------- M E M O R I A -  definicoes de opcode e palavra de memoria ----------------------

    public static class Word {    // cada posicao da memoria tem uma instrucao (ou um dado)
        public Opcode opc;    //
        public int r1;        // indice do primeiro registrador da operacao (Rs ou Rd cfe opcode na tabela)
        public int r2;        // indice do segundo registrador da operacao (Rc ou Rs cfe operacao)
        public int p;        // parametro para instrucao (k ou A cfe operacao), ou o dado, se opcode = DADO

        public Word(Opcode _opc, int _r1, int _r2, int _p) {
            opc = _opc;
            r1 = _r1;
            r2 = _r2;
            p = _p;
        }
    }
    // -------------------------------------------------------------------------------------------------------

    // -------------------------------------------------------------------------------------------------------
    // --------------------- C P U  -  definicoes da CPU -----------------------------------------------------

    public enum Opcode {
        DATA, ___,            // se memoria nesta posicao tem um dado, usa DATA, se não usada é NULO ___
        JMP, JMPI, JMPIG, JMPIL, JMPIE, JMPIM, JMPIGM, JMPILM, JMPIEM, STOP,   // desvios e parada
        ADDI, SUBI, ADD, SUB, MULT,             // matemáticos
        LDI, LDD, STD, LDX, STX, SWAP,          // movimentação
        TRAP;                                   //
    }

    public enum Interrupts {
        INT_NONE,
        INT_INVALID_INSTRUCTION,    // Nunca será usada, pois o Java não deixará compilar
        INT_INVALID_ADDRESS,        // Nossa memória tem 1024 posições
        INT_OVERFLOW,               // Nossa memória só trabalha com inteiros, ou seja de -2,147,483,648 até 2,147,483,647
        INT_SYSTEM_CALL,            // Ativa chamada de I/O pelo comando TRAP
        INT_SCHEDULER,              // Aciona o Escalonador
        INT_STOP;                   // Usada com o escalonador
    }


    // ------------------ C P U - fim ------------------------------------------------------------------------
    // -------------------------------------------------------------------------------------------------------

    // --------------------H A R D W A R E - fim -------------------------------------------------------------
    // -------------------------------------------------------------------------------------------------------

    // -------------------------------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------------------------------
    // ------------------- S O F T W A R E - inicio ----------------------------------------------------------

    // -------------------  S I S T E M A --------------------------------------------------------------------

    public InterruptHandler interruptHandler;
    public static Programas progs;
    public GerenciadorMemoria gm;
    public GerenciadorProcessos gp;
    private Escalonador escalonador;
    public int[] registradores;
    public Word instructionRegister;
    public Interrupts interrupt;
    private LinkedList<PCB> prontos;
    private Word[] memory;
    private CPU cpu;

    public Sistema(int tamMemoria, int tamanhoPagina, int maxInt, int quantidadeRegistradores, int deltaMax){   // a VM com tratamento de interrupções
        registradores = new int[quantidadeRegistradores];
        interrupt = Interrupts.INT_NONE;
        instructionRegister = new Word(Opcode.___,-1,-1,-1);

        // cria a memória
        memory = new Word[tamMemoria];
        for (int i = 0; i < tamMemoria; i++) {
            memory[i] = new Word(Opcode.___, -1, -1, -1);
        }

        this.interruptHandler = new InterruptHandler();

        // cpu
        cpu = new CPU(memory, tamanhoPagina, maxInt, deltaMax, registradores, interrupt, instructionRegister, interruptHandler);

        progs = new Programas();

        gm = new GerenciadorMemoria(memory, tamanhoPagina);
        gp = new GerenciadorProcessos(gm, gm.mem, cpu);
        this.escalonador = new Escalonador(gp);
        interruptHandler.setAttributes(gm,gp,escalonador);
    }


    // Fase 5 - Para demonstrar o funcionamento, você deve ter um sistema iterativo.
    // Uma vez que o sistema esteja funcionando, ele fica esperando comandos.
    // Os comandos possíveis são:
    // cria nomeDePrograma - cria um processo com memória alocada, PCB, etc. que fica em uma lista de processos.
    // esta chamada retorna um identificador único do processo no sistema (ex.: 1, 2, 3 …)
    // executa id - executa o processo com id fornecido. se não houver processo, retorna erro.
    // dump id - lista o conteúdo do PCB e o conteúdo de cada frame de memória do processo com id
    // dumpM inicio fim - lista os frames de memória entre início e fim, independente do processo
    // desaloca id - retira o processo id do sistema, tenha ele executado ou não

    public int cria(Word[] programa){
        int idDoProcessoCriado;
        idDoProcessoCriado = gp.criaProcesso(programa);
        if (idDoProcessoCriado==-1){
            System.out.println("Falta memoria para rodar o programa");
            return idDoProcessoCriado;
        }

        System.out.println("---------------------------------- programa carregado ");
        gm.dumpMemoriaUsada(gm.mem);
        return idDoProcessoCriado;
    }

    public void executa(int processId) {
        System.out.println("Iniciando execução do processo");
        int [] paginasAlocadas = gp.getPaginasAlocadas(processId);
        if (paginasAlocadas[0]==-1){
            System.out.println("Processo não existe");
            return;
        }
        System.out.println("Páginas alocadas");
        for (int i=0; i<paginasAlocadas.length; i++){
            System.out.println(paginasAlocadas[i] + " ");
        }

        PCB running = gp.getProcesso(processId);

        // pega o contexto do processo que está rodando
        int programCounterDoRunning = running.getProgramCounter();
        int [] paginasAlocadasDoRunning = running.getPaginasAlocadas();
        int [] registradoresdoRunning = running.getRegistradores();
        Word instructionRegisterDoRunning = running.getInstructionRegister();
        Interrupts interruptsDoRunning = running.getInterrupt();

        cpu.setContext(programCounterDoRunning, paginasAlocadasDoRunning, registradoresdoRunning, instructionRegisterDoRunning, interruptsDoRunning);
        //vm.cpu.setContext(0, paginasAlocadas, registradores, instructionRegister, interrupt);          // monitor seta contexto - pc aponta para inicio do programa
        cpu.run();                  //                         e cpu executa
        System.out.println("---------------------------------- programa executado ");
        for (int i=0; i<paginasAlocadas.length; i++){
            gm.dumpPagina(gm.mem, paginasAlocadas[i]);
        }
    }

    public void listaProcessos(){
        gp.listaProcessos();
    }

    public void executaComEscalonador() {
        System.out.println(" ");
        System.out.println("Iniciando execução dos processos prontos com escalonador");
        cpu.setEscalonadorState(true);
        prontos = gp.getProntos();
        PCB running = prontos.getFirst();
        gp.setRunning(running);

        // pega o contexto do processo que está rodando
        int programCounterDoRunning = running.getProgramCounter();
        int [] paginasAlocadasDoRunning = running.getPaginasAlocadas();
        int [] registradoresdoRunning = running.getRegistradores();
        Word instructionRegisterDoRunning = running.getInstructionRegister();
        Interrupts interruptsDoRunning = running.getInterrupt();

        cpu.setContext(programCounterDoRunning, paginasAlocadasDoRunning, registradoresdoRunning, instructionRegisterDoRunning, interruptsDoRunning);

        cpu.run();
        System.out.println("---------------------------------- Escalonador executado ");
        //gm.dumpMemoriaUsada(vm.m);
    }

    public void dump (int processId){
        System.out.println("----------- dump do processo " + processId + "------------------");
        int [] paginasAlocadas = gp.getPaginasAlocadas(processId);

        for (int i=0; i<paginasAlocadas.length; i++){
            gm.dumpPagina(gm.mem, paginasAlocadas[i]);
        }
    }

    public void dumpM (int inicio, int fim){
        System.out.println("----------- dump com inicio em " + inicio + " e fim em " + fim);
        for (int i = inicio; i<=fim; i++){
            //System.out.println("fazendo dumop da página " + i);
            gm.dumpPagina(gm.mem, i);
        }
    }

    public void desaloca (int processId){
        PCB processo = gp.getProcesso(processId);
        int [] paginasAlocadas = gp.getPaginasAlocadas(processId);
        gp.desalocaProcesso(processo);
        System.out.println("--------------Processo " + processId + " desalocado---------------");
        for (int i=0; i<paginasAlocadas.length; i++){
            gm.dumpPagina(gm.mem, paginasAlocadas[i]);
        }
    }
    // -------------------  S I S T E M A - fim --------------------------------------------------------------

}