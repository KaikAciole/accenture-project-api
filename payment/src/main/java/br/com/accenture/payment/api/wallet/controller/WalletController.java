package br.com.accenture.payment.api.wallet.controller;

import br.com.accenture.payment.api.mapper.PageRequestMapper;
import br.com.accenture.payment.api.wallet.dto.request.WalletCreateRequest;
import br.com.accenture.payment.api.wallet.dto.request.WalletCreditRequest;
import br.com.accenture.payment.api.wallet.dto.request.WalletDebitRequest;
import br.com.accenture.payment.api.wallet.dto.request.WalletTopUpRequest;
import br.com.accenture.payment.api.wallet.dto.request.WalletTransferRequest;
import br.com.accenture.payment.api.wallet.dto.response.WalletResponse;
import br.com.accenture.payment.api.wallet.dto.response.WalletTopUpResponse;
import br.com.accenture.payment.api.wallet.dto.response.WalletTransactionResponse;
import br.com.accenture.payment.api.wallet.mapper.WalletDtoMapper;
import br.com.accenture.payment.api.wallet.mapper.WalletTopUpDtoMapper;
import br.com.accenture.payment.application.service.wallet.WalletService;
import br.com.accenture.payment.application.service.wallet.WalletTopUpService;
import br.com.accenture.payment.domain.pagination.PageResult;
import br.com.accenture.payment.domain.wallet.enums.WalletOwnerType;
import br.com.accenture.payment.domain.wallet.exception.WalletNotFoundException;
import br.com.accenture.payment.domain.wallet.model.Wallet;
import br.com.accenture.payment.domain.wallet.model.WalletTopUp;
import br.com.accenture.payment.domain.wallet.model.WalletTransaction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/wallets")
@Tag(name = "Wallets", description = "Operações para gerenciamento de carteiras internas e extrato")
public class WalletController {

    private final WalletService walletService;
    private final WalletTopUpService walletTopUpService;

    public WalletController(
            WalletService walletService,
            WalletTopUpService walletTopUpService
    ) {
        this.walletService = walletService;
        this.walletTopUpService = walletTopUpService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar Wallet", description = "Cria uma carteira interna para um comprador, lojista ou empresa")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Wallet criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "409", description = "Wallet já existente para o dono informado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content)
    })
    public WalletResponse create(@RequestBody @Valid WalletCreateRequest request) {
        Wallet wallet = walletService.create(
                request.ownerId(),
                request.ownerType()
        );

        return WalletDtoMapper.toResponse(wallet);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Buscar Wallet por ID", description = "Retorna uma carteira interna pelo seu identificador")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Wallet encontrada"),
            @ApiResponse(responseCode = "404", description = "Wallet não encontrada", content = @Content),
            @ApiResponse(responseCode = "400", description = "ID inválido", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content)
    })
    public WalletResponse findById(
            @Parameter(description = "ID da Wallet")
            @PathVariable UUID id
    ) {
        Wallet wallet = walletService.findById(id);

        return WalletDtoMapper.toResponse(wallet);
    }

    @GetMapping("/owners/{ownerType}/{ownerId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Buscar Wallet por dono", description = "Retorna uma carteira pelo ID e tipo do dono")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Wallet encontrada"),
            @ApiResponse(responseCode = "404", description = "Wallet não encontrada", content = @Content),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content)
    })
    public WalletResponse findByOwner(
            @Parameter(description = "Tipo do dono da Wallet", schema = @Schema(implementation = WalletOwnerType.class))
            @PathVariable WalletOwnerType ownerType,

            @Parameter(description = "ID do dono da Wallet")
            @PathVariable UUID ownerId
    ) {
        Wallet wallet = walletService.findByOwner(ownerId, ownerType);

        return WalletDtoMapper.toResponse(wallet);
    }

    @GetMapping("/{walletId}/transactions")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Listar transações da Wallet", description = "Retorna o extrato paginado de transações de uma carteira")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transações encontradas"),
            @ApiResponse(responseCode = "404", description = "Wallet não encontrada", content = @Content),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content)
    })
    public PageResult<WalletTransactionResponse> findTransactions(
            @Parameter(description = "ID da Wallet")
            @PathVariable UUID walletId,

            @Parameter(hidden = true) Pageable pageable
    ) {
        PageResult<WalletTransaction> transactions = walletService.findTransactions(
                walletId,
                PageRequestMapper.toDomain(pageable)
        );

        return WalletDtoMapper.toTransactionPageResponse(transactions);
    }

    @GetMapping("/owners/{ownerType}/{ownerId}/transactions")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Listar transações pelo dono da Wallet",
            description = "Resolve a carteira pelo tipo+id do dono e retorna o extrato paginado. Útil para o admin ver o extrato global da empresa (COMPANY) ou de um cliente específico (CUSTOMER)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transações encontradas"),
            @ApiResponse(responseCode = "404", description = "Wallet não encontrada", content = @Content),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content)
    })
    public PageResult<WalletTransactionResponse> findTransactionsByOwner(
            @Parameter(description = "Tipo do dono da Wallet", schema = @Schema(implementation = WalletOwnerType.class))
            @PathVariable WalletOwnerType ownerType,

            @Parameter(description = "ID do dono da Wallet")
            @PathVariable UUID ownerId,

            @RequestHeader(value = "X-Customer-Id", required = false) UUID customerId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles,

            @Parameter(hidden = true) Pageable pageable
    ) {
        validateTransactionsOwnership(ownerType, ownerId, customerId, roles);

        Wallet wallet = walletService.findByOwner(ownerId, ownerType);
        PageResult<WalletTransaction> transactions = walletService.findTransactions(
                wallet.getId(),
                PageRequestMapper.toDomain(pageable)
        );

        return WalletDtoMapper.toTransactionPageResponse(transactions);
    }

    private void validateTransactionsOwnership(WalletOwnerType ownerType, UUID ownerId, UUID customerId, String roles) {
        if (customerId == null) {
            return;
        }
        boolean isAdmin = roles != null && roles.contains("ADMIN");
        if (isAdmin) {
            return;
        }
        if (ownerType != WalletOwnerType.CUSTOMER || !customerId.equals(ownerId)) {
            throw WalletNotFoundException.byOwner(ownerId, ownerType);
        }
    }

    @PostMapping("/{walletId}/top-ups")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Iniciar recarga da Wallet",
            description = "Cria uma Order de recarga no Mercado Pago e retorna os dados necessários para o frontend continuar o pagamento"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Recarga iniciada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Wallet não encontrada", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content)
    })
    public WalletTopUpResponse startTopUp(
            @Parameter(description = "ID da Wallet")
            @PathVariable UUID walletId,

            @RequestBody @Valid WalletTopUpRequest request
    ) {
        WalletTopUp topUp = walletTopUpService.startTopUp(
                walletId,
                request.customerId(),
                request.amount(),
                request.customerEmail()
        );

        return WalletTopUpDtoMapper.toResponse(topUp);
    }

    @PatchMapping("/{walletId}/credit")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Creditar saldo na Wallet", description = "Adiciona saldo em uma carteira e registra uma transação de crédito")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Saldo creditado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Wallet não encontrada", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content)
    })
    public WalletResponse credit(
            @Parameter(description = "ID da Wallet")
            @PathVariable UUID walletId,

            @RequestBody @Valid WalletCreditRequest request
    ) {
        Wallet wallet = walletService.credit(
                walletId,
                request.amount(),
                request.reason(),
                request.paymentId()
        );

        return WalletDtoMapper.toResponse(wallet);
    }

    @PatchMapping("/{walletId}/debit")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Debitar saldo da Wallet", description = "Remove saldo de uma carteira e registra uma transação de débito")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Saldo debitado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Wallet não encontrada", content = @Content),
            @ApiResponse(responseCode = "422", description = "Saldo insuficiente ou transação inválida", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content)
    })
    public WalletResponse debit(
            @Parameter(description = "ID da Wallet")
            @PathVariable UUID walletId,

            @RequestBody @Valid WalletDebitRequest request
    ) {
        Wallet wallet = walletService.debit(
                walletId,
                request.amount(),
                request.reason(),
                request.paymentId()
        );

        return WalletDtoMapper.toResponse(wallet);
    }

    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Transferir saldo entre Wallets", description = "Debita saldo da Wallet de origem e credita na Wallet de destino")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Transferência realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Wallet de origem ou destino não encontrada", content = @Content),
            @ApiResponse(responseCode = "422", description = "Saldo insuficiente ou transação inválida", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content)
    })
    public void transfer(@RequestBody @Valid WalletTransferRequest request) {
        walletService.transfer(
                request.fromOwnerId(),
                request.fromOwnerType(),
                request.toOwnerId(),
                request.toOwnerType(),
                request.amount(),
                request.paymentId()
        );
    }
}