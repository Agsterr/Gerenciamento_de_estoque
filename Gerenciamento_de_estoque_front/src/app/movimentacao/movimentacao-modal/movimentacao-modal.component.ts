import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MovimentacaoProduto, TipoMovimentacao } from '../../models/movimentacao-produto.model';
import { ProdutoService } from '../../services/produto.service';
import { ConsumidorService } from '../../services/consumidor.service';
import { Produto } from '../../models/produto.model';
import { Consumer } from '../../models/consumer.model';

export interface MovimentacaoModalData {
  modo: 'criar' | 'editar';
  movimentacao?: MovimentacaoProduto;
  tipoMovimentacao?: TipoMovimentacao | null;
  produtoId?: number;
  nomeProduto?: string;
}

@Component({
  selector: 'app-movimentacao-modal',
  templateUrl: './movimentacao-modal.component.html',
  styleUrls: ['./movimentacao-modal.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatDatepickerModule,
    MatNativeDateModule
  ]
})
export class MovimentacaoModalComponent implements OnInit {
  movimentacaoForm: FormGroup;
  tiposMovimentacao: TipoMovimentacao[] = [TipoMovimentacao.ENTRADA, TipoMovimentacao.SAIDA];
  loading = false;
  isEdicao: boolean;
  tituloModal: string;
  produtos: Produto[] = [];
  consumidores: Consumer[] = [];
  readonly TipoMovimentacao = TipoMovimentacao;

  constructor(
    private dialogRef: MatDialogRef<MovimentacaoModalComponent>,
    @Inject(MAT_DIALOG_DATA) public data: MovimentacaoModalData,
    private fb: FormBuilder,
    private produtoService: ProdutoService,
    private consumidorService: ConsumidorService
  ) {
    this.isEdicao = data.modo === 'editar';
    this.tituloModal = this.isEdicao ? 'Editar Movimentação' : 'Nova Movimentação';
    const mov = data.movimentacao;

    this.movimentacaoForm = this.fb.group({
      produtoId: [
        this.isEdicao ? mov?.produtoId : (data.produtoId || null),
        Validators.required
      ],
      tipo: [
        this.isEdicao ? mov?.tipo : (data.tipoMovimentacao || TipoMovimentacao.ENTRADA),
        Validators.required
      ],
      quantidade: [
        this.isEdicao ? mov?.quantidade : null,
        [Validators.required, Validators.min(1), Validators.max(999999)]
      ],
      consumidorId: [mov?.consumidorId || null],
      dataHora: [
        this.isEdicao && mov?.dataHora ? new Date(mov.dataHora) : new Date(),
        Validators.required
      ],
      observacoes: ['', Validators.maxLength(500)]
    });
  }

  ngOnInit(): void {
    this.produtoService.listarProdutos(0, 500).subscribe({
      next: (res) => this.produtos = res.content || [],
      error: () => this.produtos = []
    });
    this.consumidorService.listarConsumidores(0, 500).subscribe({
      next: (list) => this.consumidores = list || [],
      error: () => this.consumidores = []
    });
    this.movimentacaoForm.get('tipo')?.valueChanges.subscribe(() => this.atualizarValidacaoConsumidor());
    this.atualizarValidacaoConsumidor();
  }

  private atualizarValidacaoConsumidor(): void {
    const ctrl = this.movimentacaoForm.get('consumidorId');
    if (!ctrl) return;
    if (this.movimentacaoForm.get('tipo')?.value === TipoMovimentacao.SAIDA && !this.isEdicao) {
      ctrl.setValidators([Validators.required]);
    } else {
      ctrl.clearValidators();
    }
    ctrl.updateValueAndValidity();
  }

  onSubmit(): void {
    if (this.movimentacaoForm.invalid) {
      this.movimentacaoForm.markAllAsTouched();
      return;
    }
    this.loading = true;
    const formValue = this.movimentacaoForm.value;
    const produto = this.produtos.find(p => p.id === formValue.produtoId);
    const movimentacao: MovimentacaoProduto = {
      id: this.isEdicao ? this.data.movimentacao!.id : 0,
      produtoId: formValue.produtoId,
      tipo: formValue.tipo,
      quantidade: formValue.quantidade,
      dataHora: formValue.dataHora.toISOString(),
      orgId: this.isEdicao ? this.data.movimentacao!.orgId : 0,
      nomeProduto: produto?.nome || '',
      consumidorId: formValue.consumidorId || undefined,
      entregaId: this.isEdicao ? this.data.movimentacao?.entregaId : undefined
    };

    this.dialogRef.close({
      success: true,
      data: movimentacao,
      observacoes: formValue.observacoes,
      modo: this.data.modo
    });
  }

  onCancel(): void {
    this.dialogRef.close({ success: false });
  }

  getTipoDisplayName(tipo: TipoMovimentacao): string {
    return tipo === TipoMovimentacao.ENTRADA ? 'Entrada' : 'Saída';
  }

  get produtoId() { return this.movimentacaoForm.get('produtoId'); }
  get tipo() { return this.movimentacaoForm.get('tipo'); }
  get quantidade() { return this.movimentacaoForm.get('quantidade'); }
  get consumidorId() { return this.movimentacaoForm.get('consumidorId'); }
  get dataHora() { return this.movimentacaoForm.get('dataHora'); }
  get observacoes() { return this.movimentacaoForm.get('observacoes'); }
}
