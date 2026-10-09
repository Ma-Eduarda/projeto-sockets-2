import base64
import socket

try:
    sock = socket.create_connection(("127.0.0.1", 12345))
    entrada = sock.makefile("r")
    saida = sock.makefile("w")

    status = entrada.readline().strip()
    if status.startswith("ERRO"):
        print(status)
        sock.close()
        raise SystemExit

except (ConnectionError, OSError):
    print("Servidor encerrou a conexao.")
    raise SystemExit

print("Conectado ao servidor.")
print()

try:
    while True:
        print("Menu:")
        print("1 - Divisao")
        print("2 - Adicao")
        print("3 - Subtracao")
        print("4 - Pedir imagem ao servidor")
        print("0 - Encerrar")
        print()
        opcao = input("Opcao: ")

        if opcao in ("1", "2", "3"):
            numero1 = input("Digite o primeiro numero: ").strip().replace(",", ".")
            numero2 = input("Digite o segundo numero: ").strip().replace(",", ".")

            mensagem = opcao + " " + numero1 + " " + numero2
            
        elif opcao == "4": mensagem = "4"

        elif opcao == "0": mensagem = "0"

        else:
            print("Opcao invalida.")
            print()
            continue

        saida.write(mensagem + "\n")
        saida.flush()

        resposta = entrada.readline().strip()
        if resposta == "":
            print("Servidor desconectado.")
            break

        if opcao == "4" and not resposta.startswith("ERRO"):
            with open("carameloPedreiro.jpg", "wb") as arquivo:
                arquivo.write(base64.b64decode(resposta))
            print("Imagem recebida!")
            
        else:
            print("Resultado:", resposta)
            print()

        if opcao == "0":
            break

        if resposta == "ERRO: servidor cheio":
            break
        
except KeyboardInterrupt:
    print("\nCONEXAO ENCERRADA!")
    
except (ConnectionError, OSError):
    print("Servidor desconectado. Conexao encerrada.")
    
finally:
    try:
        entrada.close()
    except OSError:
        pass

    try:
        saida.close()
    except OSError:
        pass

    try:
        sock.close()
    except OSError:
        pass