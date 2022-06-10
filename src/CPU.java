public class CPU {
    // característica do processador: contexto da CPU ...
    private int pc;            // ... composto de program counter,
    private Sistema.Word ir;            // instruction register,
    private int[] reg;        // registradores da CPU
    public int maxInt;          // criado para podermos simular overflow
    private int[] paginasAlocadas;
    private int tamPaginaMemoria;

    // usado pelo escalonador
    int delta;
    int deltaMax;
    boolean escalonadorState;

    // cria variável interrupção
    public Sistema.Interrupts interrupts;

    private InterruptHandler interruptHandler;

    private Sistema.Word[] m;   // CPU acessa MEMORIA, guarda referencia 'm' a ela. memoria nao muda. ee sempre a mesma.

    public CPU(Sistema.Word[] _m, int tamPaginaMemoria, int maxInt, int deltaMax, int [] reg, Sistema.Interrupts interrupts,
               Sistema.Word ir, InterruptHandler interruptHandler) {     // ref a MEMORIA e interrupt handler passada na criacao da CPU
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
        this.interruptHandler = interruptHandler;
    }

    public void setContext(int _pc, int [] paginasAlocadas, int [] registradores, Sistema.Word instructionRegister, Sistema.Interrupts interrupt) {  // no futuro esta funcao vai ter que ser
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

    public Sistema.Interrupts getInterrupts(){
        return interrupts;
    }

    public int [] getReg(){
        return reg;
    }

    public int getPc(){
        return pc;
    }

    public Sistema.Word getIr(){
        return ir;
    }

    private void dump(Sistema.Word w) {
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
            interrupts = Sistema.Interrupts.INT_INVALID_INSTRUCTION;
            return false;
        }
        return true;
    }

    private boolean isAddressValid(int address) {
        if (address < 0 || address >= m.length) {
            interrupts = Sistema.Interrupts.INT_INVALID_ADDRESS;
            return false;
        }
        return true;
    }

    private boolean isNumberValid(int number) {
        if (number < maxInt * -1 || number > maxInt) {
            interrupts = Sistema.Interrupts.INT_OVERFLOW;
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
                        m[traduzEndereco(ir.p)].opc = Sistema.Opcode.DATA;
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
                        interrupts = Sistema.Interrupts.INT_OVERFLOW;
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
                        interrupts = Sistema.Interrupts.INT_OVERFLOW;
                        pc++;
                        break;
                    }


                case STX: // [Rd] ←Rs
                    if (isRegisterValid(ir.r1) && isRegisterValid(ir.r2) && isAddressValid(traduzEndereco(reg[ir.r1]))) {
                        m[traduzEndereco(reg[ir.r1])].opc = Sistema.Opcode.DATA;
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
                        interrupts = Sistema.Interrupts.INT_OVERFLOW;
                        pc++;
                        break;
                    }

                case SUBI: // Rd ← Rd - k
                    if (isRegisterValid(ir.r1) && isNumberValid(reg[ir.r1]) && isNumberValid(ir.p) && isNumberValid(reg[ir.r1] - ir.p)) {
                        reg[ir.r1] = reg[ir.r1] - ir.p;
                        pc++;
                        break;
                    } else {
                        interrupts = Sistema.Interrupts.INT_OVERFLOW;
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
                    interrupts = Sistema.Interrupts.INT_SYSTEM_CALL;
                    pc++;
                    break;

                case DATA:
                    pc++;
                    break;

                default:
                    // opcode desconhecido
                    interrupts = Sistema.Interrupts.INT_INVALID_INSTRUCTION;
            }

            // VERIFICA INTERRUPÇÃO !!! - TERCEIRA FASE DO CICLO DE INSTRUÇÕES
            if (ir.opc == Sistema.Opcode.STOP) {
                if (escalonadorState==true){
                    interrupts = Sistema.Interrupts.INT_STOP;
                }
                else
                    break; // break sai do loop da cpu
            }

            // Aciona o Escalonador
            if (delta==deltaMax && escalonadorState==true){
                delta=0;
                interrupts = Sistema.Interrupts.INT_SCHEDULER;
            }

            if (interrupts != Sistema.Interrupts.INT_NONE) {
                run = interruptHandler.handleInterrupt(reg, ir, m, pc, interrupts);
                interrupts = Sistema.Interrupts.INT_NONE; // sai da chamada de sistema. talvez seja preciso criar um handler pra system call
            }
        }
    }
}