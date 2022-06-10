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

    public class CPU {
        // característica do processador: contexto da CPU ...
        private int pc;            // ... composto de program counter,
        private Word ir;            // instruction register,
        private int[] reg;        // registradores da CPU
        public int maxInt;          // criado para podermos simular overflow
        private int[] paginasAlocadas;
        private int tamPaginaMemoria;

        // usado pelo escalonador
        int delta;
        int deltaMax;
        boolean escalonadorState;

        // cria variável interrupção
        public Interrupts interrupts;

        private Word[] m;   // CPU acessa MEMORIA, guarda referencia 'm' a ela. memoria nao muda. ee sempre a mesma.

        public CPU(Word[] _m, int tamPaginaMemoria, int maxInt, int deltaMax, int [] reg, Interrupts interrupts, Word ir) {     // ref a MEMORIA e interrupt handler passada na criacao da CPU
            m = _m;                // usa o atributo 'm' para acessar a memoria.
            reg = new int[10];        // aloca o espaço dos registradores
            this.maxInt = maxInt;          // números aceitos -100_000 até 100_000
            this.tamPaginaMemoria = tamPaginaMemoria;
            this.reg = reg;
            this.interrupts = interrupts;
            this.ir = ir;

            delta = 0;
            this.deltaMax = deltaMax;
            escalonadorState = false;
        }

        public void setContext(int _pc, int [] paginasAlocadas, int [] registradores, Word instructionRegister, Interrupts interrupt) {  // no futuro esta funcao vai ter que ser
            pc = _pc;                                   // limite e pc (deve ser zero nesta versão)
            this.paginasAlocadas = paginasAlocadas;
            this.reg = registradores;
            ir = instructionRegister;
            this.interrupts = interrupt;

            // debug
            /*
            System.out.println("Novo contexto de CPU setado pelo setContext da CPU");
            System.out.println("pc em: " + pc);
            System.out.print("Páginas alocadas: ");
            for (int i=0; i<paginasAlocadas.length; i++){
                System.out.println(paginasAlocadas[i] + " ");
            }
            System.out.println("");
            System.out.println("Registradores recebidos pelo setContext");
            for (int i = 0; i < reg.length; i++) {
                System.out.print("r" + i);
                System.out.print(": " + reg[i] + "     ");
            }
            ;
            System.out.println("");
             */

        }

        public void setEscalonadorState(boolean state){
            this.escalonadorState = state;
        }

        public Interrupts getInterrupts(){
            return interrupts;
        }

        public int [] getReg(){
            return reg;
        }

        public int getPc(){
            return pc;
        }

        public Word getIr(){
            return ir;
        }

        private void dump(Word w) {
            System.out.print("[ ");
            System.out.print(w.opc);
            System.out.print(", ");
            System.out.print(w.r1);
            System.out.print(", ");
            System.out.print(w.r2);
            System.out.print(", ");
            System.out.print(w.p);
            System.out.println("  ] ");
        }

        private void showState() {
            System.out.println("       " + pc);
            System.out.print("           ");
            for (int i = 0; i < reg.length; i++) {
                System.out.print("r" + i);
                System.out.print(": " + reg[i] + "     ");
            }
            ;
            System.out.println("");
            System.out.print("           ");
            dump(ir);
        }


        private boolean isRegisterValid(int register) {
            if (register < 0 || register >= reg.length) {
                interrupts = Interrupts.INT_INVALID_INSTRUCTION;
                return false;
            }
            return true;
        }

        private boolean isAddressValid(int address) {
            if (address < 0 || address >= m.length) {
                interrupts = Interrupts.INT_INVALID_ADDRESS;
                return false;
            }
            return true;
        }

        private boolean isNumberValid(int number) {
            if (number < maxInt * -1 || number > maxInt) {
                interrupts = Interrupts.INT_OVERFLOW;
                return false;
            }
            return true;
        }

        public int traduzEndereco (int endereco){
            // debug
            /*
            System.out.println("Traduzindo endereço: " + endereco);
            System.out.print("Páginas alocadas usadas pelo tradutor de endereço: ");
            for (int i=0; i<paginasAlocadas.length; i++){
                System.out.println(paginasAlocadas[i] + " ");
            }
            System.out.println("");
             */

            try {
                return (paginasAlocadas[(endereco / tamPaginaMemoria)] * tamPaginaMemoria) + (endereco % tamPaginaMemoria);

            } catch(ArrayIndexOutOfBoundsException e) {
                //System.out.println("Retorno -1 do traduz");
                return -1;
            }
        }

        public void run() {

            boolean run = true;
            while (run) {
                // FETCH

                //System.out.println("rodando a cpu");

                ir = m[traduzEndereco(pc)];    // busca posicao da memoria apontada por pc, guarda em ir

                delta++;
                //System.out.println("Delta em " + delta);

                //só para debug
                //showState();

                // EXECUTA INSTRUCAO NO ir
                switch (ir.opc) { // para cada opcode, sua execução

                    case LDI: // Rd ← k
                        if (isRegisterValid(ir.r1) && isNumberValid(ir.p)) {
                            reg[ir.r1] = ir.p;
                            pc++;
                            break;
                        } else
                            break;

                    case LDD: // Rd ← [A]
                        if (isRegisterValid(ir.r1) && isAddressValid(traduzEndereco(ir.p)) && isNumberValid(m[ir.p].p)) {
                            reg[ir.r1] = m[traduzEndereco(ir.p)].p;
                            pc++;
                            break;
                        } else
                            break;

                    case STD: // [A] ← Rs
                        if (isRegisterValid(ir.r1) && isAddressValid(traduzEndereco(ir.p)) && isNumberValid(reg[ir.r1])) {
                            m[traduzEndereco(ir.p)].opc = Opcode.DATA;
                            m[traduzEndereco(ir.p)].p = reg[ir.r1];
                            pc++;
                            break;
                        } else
                            break;

                    case ADD: // Rd ← Rd + Rs
                        if (isRegisterValid(ir.r2) && isRegisterValid(ir.r1) && isNumberValid(reg[ir.r1]) && isNumberValid(reg[ir.r2]) && isNumberValid(reg[ir.r1] + reg[ir.r2])) {
                            reg[ir.r1] = reg[ir.r1] + reg[ir.r2];
                            pc++;
                            break;
                        } else {
                            interrupts = Interrupts.INT_OVERFLOW;
                            pc++;
                            break;
                        }

                    case MULT: // Rd ← Rd * Rs
                        if (isRegisterValid(ir.r2) && isRegisterValid(ir.r1)) {
                            if (isNumberValid(reg[ir.r1] * reg[ir.r2]) && isNumberValid(reg[ir.r1]) && isNumberValid(reg[ir.r2])) {
                                reg[ir.r1] = reg[ir.r1] * reg[ir.r2];
                                pc++;
                                break;
                            } else {
                                pc++;
                                break;
                            }
                        } else
                            break;

                    case ADDI: // Rd ← Rd + k
                        if (isRegisterValid(ir.r1) && isNumberValid(reg[ir.r1]) && isNumberValid(ir.p) && isNumberValid(reg[ir.r1] + ir.p)) {
                            reg[ir.r1] = reg[ir.r1] + ir.p;
                            pc++;
                            break;
                        } else {
                            interrupts = Interrupts.INT_OVERFLOW;
                            pc++;
                            break;
                        }


                    case STX: // [Rd] ←Rs
                        if (isRegisterValid(ir.r1) && isRegisterValid(ir.r2) && isAddressValid(traduzEndereco(reg[ir.r1]))) {
                            m[traduzEndereco(reg[ir.r1])].opc = Opcode.DATA;
                            m[traduzEndereco(reg[ir.r1])].p = reg[ir.r2];
                            pc++;
                            break;
                        } else
                            break;

                    case LDX: // Rd ← [Rs]
                        if (isRegisterValid(ir.r1) && isRegisterValid(ir.r2) && isAddressValid(traduzEndereco(reg[ir.r2])) && isNumberValid(m[reg[ir.r2]].p)) {
                            reg[ir.r1] = m[traduzEndereco(reg[ir.r2])].p;
                            pc++;
                            break;
                        } else
                            break;

                    case SUB: // Rd ← Rd - Rs
                        if (isRegisterValid(ir.r1) && isRegisterValid(ir.r2) && isNumberValid(reg[ir.r2]) && isNumberValid(reg[ir.r1]) && isNumberValid(reg[ir.r1] - reg[ir.r2])) {
                            reg[ir.r1] = reg[ir.r1] - reg[ir.r2];
                            pc++;
                            break;
                        } else {
                            interrupts = Interrupts.INT_OVERFLOW;
                            pc++;
                            break;
                        }

                    case SUBI: // Rd ← Rd - k
                        if (isRegisterValid(ir.r1) && isNumberValid(reg[ir.r1]) && isNumberValid(ir.p) && isNumberValid(reg[ir.r1] - ir.p)) {
                            reg[ir.r1] = reg[ir.r1] - ir.p;
                            pc++;
                            break;
                        } else {
                            interrupts = Interrupts.INT_OVERFLOW;
                            pc++;
                            break;
                        }

                    case JMP: //  PC ← k
                        if (isAddressValid(traduzEndereco(ir.p))) {
                            pc = ir.p;
                            break;
                        } else
                            break;

                    case JMPI: //  PC ← Rs
                        if (isRegisterValid(traduzEndereco(ir.r1)) && isAddressValid(traduzEndereco(reg[ir.r1]))) {
                            pc = reg[ir.r1];
                            break;
                        } else
                            break;


                    case JMPIG: // If Rc > 0 Then PC ← Rs Else PC ← PC +1
                        if (isRegisterValid(ir.r2) && isRegisterValid(ir.r1) && isAddressValid(traduzEndereco(reg[ir.r1]))) {
                            if (reg[ir.r2] > 0) {
                                pc = reg[ir.r1];
                            } else {
                                pc++;
                            }
                            break;
                        } else
                            break;

                    case JMPIGM: // If Rc > 0 Then PC ← [A] Else PC ← PC +1
                        if (isRegisterValid(ir.r2) && isAddressValid(traduzEndereco(ir.p)) && isAddressValid(traduzEndereco(m[ir.p].p))) {
                            if (reg[ir.r2] > 0) {
                                pc = m[traduzEndereco(ir.p)].p;
                            } else {
                                pc++;
                            }
                            break;
                        } else
                            break;

                    case JMPILM: // If Rc < 0 Then PC ← [A] Else PC ← PC +1
                        if (isRegisterValid(ir.r2) && isAddressValid(traduzEndereco(ir.p)) && isAddressValid(traduzEndereco(m[ir.p].p))) {
                            if (reg[ir.r2] < 0) {
                                pc = m[traduzEndereco(ir.p)].p;
                            } else {
                                pc++;
                            }
                            break;
                        } else
                            break;

                    case JMPIEM: // If Rc = 0 Then PC ← [A] Else PC ← PC +1
                        if (isRegisterValid(ir.r2) && isAddressValid(traduzEndereco(ir.p)) && isAddressValid(traduzEndereco(m[ir.p].p))) {
                            if (reg[ir.r2] == 0) {
                                pc = m[traduzEndereco(ir.p)].p;
                            } else {
                                pc++;
                            }
                            break;
                        } else
                            break;


                    case JMPIE: // If Rc = 0 Then PC ← Rs Else PC ← PC +1
                        if (isRegisterValid(ir.r1) && isRegisterValid(ir.r2) && isAddressValid(traduzEndereco(reg[ir.r1]))) {
                            if (reg[ir.r2] == 0) {
                                pc = reg[ir.r1];
                            } else {
                                pc++;
                            }
                            break;
                        } else
                            break;

                    case JMPIL: //  PC ← Rs
                        if (isRegisterValid(ir.r1) && isRegisterValid(ir.r2) && isAddressValid(traduzEndereco(reg[ir.r1]))) {
                            if (reg[ir.r2] < 0) {
                                pc = reg[ir.r1];
                            } else {
                                pc++;
                            }
                            break;
                        } else
                            break;

                    case JMPIM: //  PC ← [A]
                        if (isAddressValid(traduzEndereco(m[ir.p].p)) && isAddressValid(traduzEndereco(ir.p))) {
                            pc = m[traduzEndereco(ir.p)].p;
                            break;
                        } else
                            break;

                    case SWAP: // t <- r1; r1 <- r2; r2 <- t
                        if (isRegisterValid(ir.r1) && isRegisterValid(ir.r2) && isNumberValid(reg[ir.r1]) && isNumberValid(reg[ir.r2])) {
                            int temp;
                            temp = reg[ir.r1];
                            reg[ir.r1] = reg[ir.r2];
                            reg[ir.r2] = temp;
                            pc++;
                            break;
                        } else
                            break;

                    case STOP: // por enquanto, para execucao
                        break;

                    case TRAP:
                        interrupts = Interrupts.INT_SYSTEM_CALL;
                        pc++;
                        break;

                    case DATA:
                        pc++;
                        break;

                    default:
                        // opcode desconhecido
                        interrupts = Interrupts.INT_INVALID_INSTRUCTION;
                }

                // VERIFICA INTERRUPÇÃO !!! - TERCEIRA FASE DO CICLO DE INSTRUÇÕES
                if (ir.opc == Opcode.STOP) {
                    if (escalonadorState==true){
                        interrupts = Interrupts.INT_STOP;
                    }
                    else
                        break; // break sai do loop da cpu
                }

                // Aciona o Escalonador
                if (delta==deltaMax && escalonadorState==true){
                    delta=0;
                    interrupts = Interrupts.INT_SCHEDULER;
                }

                if (interrupts != Interrupts.INT_NONE) {
                    run = interruptHandler.handleInterrupt(reg, ir, m, pc, interrupts);
                    interrupts = Interrupts.INT_NONE; // sai da chamada de sistema. talvez seja preciso criar um handler pra system call
                }
            }
        }
    }
    // ------------------ C P U - fim ------------------------------------------------------------------------
    // -------------------------------------------------------------------------------------------------------

    // --------------------H A R D W A R E - fim -------------------------------------------------------------
    // -------------------------------------------------------------------------------------------------------

    // -------------------------------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------------------------------
    // ------------------- S O F T W A R E - inicio ----------------------------------------------------------

    // -------------------------------------------  funcoes de um monitor



    public class GerenciadorMemoria {

        private Word[] mem;
        private int tamPagina;
        private int tamFrame;
        private int nroFrames;
        private boolean[] tabelaPaginas;
        //private int quantidadeDePaginasUsadas; // só para debug
        public int [] framesAlocados;

        public GerenciadorMemoria(Word[] mem, int tamPagina) {
            this.mem = mem;
            this.tamPagina = tamPagina;
            tamFrame = tamPagina;
            nroFrames = mem.length / tamPagina;
            tabelaPaginas = initFrames();
            //quantidadeDePaginasUsadas = 0;
        }

        public int getTamPagina(){
            return tamPagina;
        }

        private boolean[] initFrames() {
            boolean[] free = new boolean[nroFrames];
            for (int i = 0; i < nroFrames; i++) {
                free[i] = true;
            }

            //mockando frames ocupados
            free[0]=false;
            free[2]=false;

            return free;
        }

        public void dump(Word w) {
            System.out.print("[ ");
            System.out.print(w.opc);
            System.out.print(", ");
            System.out.print(w.r1);
            System.out.print(", ");
            System.out.print(w.r2);
            System.out.print(", ");
            System.out.print(w.p);
            System.out.println("  ] ");
        }

        public void dumpMem(Word[] m, int ini, int fim) {
            for (int i = ini; i < fim; i++) {
                System.out.print(i);
                System.out.print(":  ");
                dump(m[i]);
            }
        }

        public int getQuantidadePaginasUsadas(){
            int quantidade = 0;
            for (int i=0; i<tabelaPaginas.length; i++){
                if (tabelaPaginas[i]==false) quantidade++;
            }
            return quantidade;
        }

        public void dumpMemoriaUsada(Word[] m) {
            int fim = getQuantidadePaginasUsadas() * tamPagina;
            for (int i = 0; i < fim; i++) {
                System.out.print(i);
                System.out.print(":  ");
                dump(m[i]);
            }
        }

        public void dumpPagina (Word[]m, int pagina){
            int ini = tamPagina * pagina;
            int fim = ini + tamPagina;
            for (int i = ini; i < fim; i++) {
                System.out.print(i);
                System.out.print(":  ");
                dump(m[i]);
            }
        }


        // retorna -1 se não conseguir alocar, ou um array com os frames alocadas
        public int[] aloca(Word[] programa) {
            int quantidadePaginas = programa.length / tamPagina;
            if (programa.length % tamPagina > 0) quantidadePaginas++; // vê se ainda tem código além da divisão inteira
            framesAlocados = new int[quantidadePaginas];
            int indiceAlocado = 0;

            // testa se tem espaço para alocar o programa
            int framesLivres =0;
            for (int i = 0; i < nroFrames; i++) {
                if (tabelaPaginas[i]) //vê se o frame está vazio e conta 1
                    framesLivres++;
            }

            // se não existe memória suficiente retorna um array com -1
            if (framesLivres <= quantidadePaginas){
                framesAlocados [0] = -1;
                return framesAlocados;
            }

            // aloca os frames
            for (int i = 0; i < nroFrames; i++) {
                if (quantidadePaginas == 0) break;
                if (tabelaPaginas[i]) { //vê se o frame está vazio e aloca o programa ali
                    tabelaPaginas[i] = false; // marca o frame como ocupado
                    framesAlocados[indiceAlocado] = i;
                    indiceAlocado++;
                    quantidadePaginas--;
                }
            }
            return framesAlocados;
        }

        public int[] getFramesAlocados(){
            return framesAlocados;
        }

        public boolean[] getTabelaDePaginas(){
            return tabelaPaginas;
        }

        public void desaloca(PCB processo){
            int[] paginas = processo.getPaginasAlocadas();

            System.out.println(" ");
            System.out.println("------ Estado da memória onde estava o programa que foi desalocado ---------");
            System.out.println("------ Só para mostrar que memória ficou limpa --------");
            for(int i = 0; i < paginas.length; i ++) {
                dumpPagina(mem, paginas[i]);
            }

            // libera os frames
            for(int i = 0; i < paginas.length; i ++) {
                tabelaPaginas[paginas[i]] = true;
            }
        }
    }

    public class GerenciadorProcessos {
        private GerenciadorMemoria gm;
        private Word[] memory;

        public LinkedList<PCB> prontos;
        private LinkedList<PCB> bloqueados;
        public PCB running;

        private int process_id;
        public int posicaoEscalonador;
        public int tamPagina;

        public GerenciadorProcessos(GerenciadorMemoria gm, Word[] memory) {
            process_id=0;
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
        public int criaProcesso(Word [] programa){
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
            PCB processo = new PCB(process_id, paginasAlocadas, 0, new int[10], new Word(Opcode.___,-1,-1,-1), Interrupts.INT_NONE);
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
                    memory[j].opc = Opcode.___;
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
            Word instructionRegisterDoRunning = running.getInstructionRegister();
            Interrupts interruptsDoRunning = running.getInterrupt();

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

        public void runEscalonador(int programCounter, int [] registradores, Word instructionRegister, Interrupts interrupt, int[] paginasAlocadas){
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
    private LinkedList<PCB> bloqueados;
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

        // cpu
        cpu = new CPU(memory, tamanhoPagina, maxInt, deltaMax, registradores, interrupt, instructionRegister);


        progs = new Programas();
        prontos = new LinkedList();
        bloqueados = new LinkedList();
        gm = new GerenciadorMemoria(memory, tamanhoPagina);
        gp = new GerenciadorProcessos(gm, gm.mem);
        interruptHandler = new InterruptHandler(gm, gp);

        this.escalonador = new Escalonador(prontos, cpu);
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

    // Fase 6 - Escalonador rodando no Sistema
    public void runEscalonador() {
        System.out.println("Iniciando Escalonador");
        escalonador.run();
    }

    public class Escalonador {

        private LinkedList<PCB> prontos;
        private int pointer;
        private PCB runningProcess;
        private CPU cpu;

        public Escalonador(LinkedList<PCB> prontos, CPU cpu) {
            this.prontos = prontos;
            this.pointer = 0;
            this.cpu = cpu;
        }

        public void run() {
            while(true){
                if(prontos.isEmpty()==false) break; //Se esvaziou lista de prontos, termina.
                PCB pcb = prontos.get(pointer);
                this.runningProcess = pcb;
                int old = pointer;
                pointer = pointer + 1;
                prontos.remove(old);
                cpu.setContext(cpu.getPc(), pcb.getPaginasAlocadas(), cpu.getReg(), cpu.getIr(), cpu.getInterrupts());
            }
        }

        public PCB getRunningProcess() {
            return runningProcess;
        }

        public void setRunningProcessAsNull(){
            this.runningProcess = null;
        }

        public LinkedList<PCB> getProntos() {
            return prontos;
        }
    }

    // -------------------  S I S T E M A - fim --------------------------------------------------------------

}