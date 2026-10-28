import base64
import queue
import socket
import threading

IP_SERVIDOR = "127.0.0.1"
NOME_CLIENTE = "Cliente-Python"   

try:
    sock = socket.create_connection((IP_SERVIDOR, 12345))
    entrada = sock.makefile("r", encoding="utf-8")
    saida = sock.makefile("w", encoding="utf-8")

    status = entrada.readline().strip()
    if status == "" or status.startswith("ERRO"):
        print(status or "Servidor encerrou a conexao.")
        sock.close()
        raise SystemExit

except (ConnectionError, OSError):
    print("Servidor encerrou a conexao.")
    raise SystemExit

saida.write("NOME " + NOME_CLIENTE + "\n")
saida.flush()

resposta_nome = entrada.readline().strip()  
meu_nome = resposta_nome[len("NOME_OK "):]
print(f" Conectado ao servidor como: {meu_nome}")
respostas = queue.Queue()

def leitora():
    try:
        while True:
            linha = entrada.readline()
            if linha == "":
                break
            linha = linha.strip()
            if linha.startswith("MSG "):
                print()
                print("Mensagem de " + linha[len("MSG "):])
                print()
            else:
                respostas.put(linha)
    except (ConnectionError, OSError, ValueError):
        pass
    finally:
        respostas.put("CONEXAO ENCERRADA!")


thread_leitora = threading.Thread(target=leitora, daemon=True)
thread_leitora.start()

try:
    while True:
        print("")
        print(f"Menu:")
        print("1 - Divisao")
        print("2 - Adicao")
        print("3 - Subtracao")
        print("4 - Pedir imagem ao servidor")
        print("5 - Mandar mensagem para outro cliente")
        print("0 - Encerrar")
        print("")
        opcao = input("Opcao: ")

        if opcao in ("1", "2", "3"):
            numero1 = input("Digite o primeiro numero: ").strip().replace(",", ".")
            numero2 = input("Digite o segundo numero: ").strip().replace(",", ".")
            mensagem = opcao + " " + numero1 + " " + numero2

        elif opcao == "4":
            mensagem = "4"

        elif opcao == "5":
            saida.write("5\n")
            saida.flush()
            lista = respostas.get()  
            if not lista.startswith("OK:"):
                print("Servidor encerrou a conexao.")
                break
            texto = lista[3:].strip()
            if texto == "":
                print("Nenhum outro cliente conectado.")
                print()
                continue

            nomes = texto.split(", ")
            print("Clientes conectados:")
            for i, nome in enumerate(nomes, 1):
                print(f"{i} - {nome}")
            print("0 - Voltar")
            escolha = input("Destinatario: ").strip()
            if escolha == "0":
                print()
                continue
            if not escolha.isdigit() or not (1 <= int(escolha) <= len(nomes)):
                print("Opcao invalida.")
                print()
                continue
            destino = nomes[int(escolha) - 1]

            conteudo = input(f"Mensagem para {destino}: ").strip()
            if conteudo == "":
                print("Mensagem vazia, nada enviado.")
                print()
                continue
            mensagem = "5 " + destino + " " + conteudo

        elif opcao == "0":
            saida.write("0\n")
            saida.flush()
            break

        else:
            print("Opcao invalida.")
            print()
            continue

        saida.write(mensagem + "\n")
        saida.flush()
        resposta = respostas.get()

        if resposta == "CONEXAO ENCERRADA!":
            print("Servidor encerrou a conexao.")
            break

        if opcao == "4" and not resposta.startswith("ERRO"):
            with open("carameloPedreiro.jpg", "wb") as arquivo:
                arquivo.write(base64.b64decode(resposta))
            print("Imagem recebida!")

        elif opcao == "5":
            if resposta.startswith("OK:"):
                print("Mensagem enviada.")
            else:
                print(resposta)

        else:
            print("Resultado:", resposta)

except KeyboardInterrupt:
    print("\nCONEXAO ENCERRADA!")

except (ConnectionError, OSError):
    print("Servidor desconectado. Conexao encerrada.")

finally:
    try:
        sock.shutdown(socket.SHUT_RDWR)
    except OSError:
        pass

    thread_leitora.join(timeout=1)
    try:
        saida.close()
    except OSError:
        pass
    try:
        entrada.close()
    except OSError:
        pass
    sock.close()
