import { Component, Inject } from '@angular/core';
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

export interface MovimentacaoModalData {
  tipoMovimentacao: TipoMovimentacao;
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
export class MovimentacaoModalComponent {
  movimentacaoForm: FormGroup;
  tiposMovimentacao: TipoMovimentacao[] = [TipoMovimentacao.ENTRADA, TipoMovimentacao.SAIDA];
  loading = false;

  constructor(
    private dialogRef: MatDialogRef<MovimentacaoModalComponent>,
    @Inject(MAT_DIALOG_DATA) public data: MovimentacaoModalData,
    private fb: FormBuilder
  ) {
    this.movimentacaoForm = this.fb.group({
      produtoId: [data.produtoId || '', [Validators.required, Validators.min(1)]],
      nomeProduto: [data.nomeProduto || '', [Validators.required, Validators.minLength(2)]],
      tipo: [data.tipoMovimentacao, Validators.required],
      quantidade: ['', [Validators.required, Validators.min(1), Validators.max(999999)]],
      dataHora: [new Date(), Validators.required],
      observacoes: ['', Validators.maxLength(500)]
    });
  }

  onSubmit(): void {
    if (this.movimentacaoForm.valid) {
      this.loading = true;
      
      const formValue = this.movimentacaoForm.value;
      const novaMovimentacao: MovimentacaoProduto = {
        id: 0,
        produtoId: formValue.produtoId,
        tipo: formValue.tipo,
        quantidade: formValue.quantidade,
        dataHora: formValue.dataHora.toISOString(),
        orgId: 1, // TODO: Pegar do contexto do usuário
        nomeProduto: formValue.nomeProduto
      };

      this.dialogRef.close({
        success: true,
        data: novaMovimentacao,
        observacoes: formValue.observacoes
      });
    }
  }

  onCancel(): void {
    this.dialogRef.close({ success: false });
  }

  getTipoDisplayName(tipo: TipoMovimentacao): string {
    return tipo === TipoMovimentacao.ENTRADA ? 'Entrada' : 'Saída';
  }

  // Getters para facilitar o acesso no template
  get produtoId() { return this.movimentacaoForm.get('produtoId'); }
  get nomeProduto() { return this.movimentacaoForm.get('nomeProduto'); }
  get tipo() { return this.movimentacaoForm.get('tipo'); }
  get quantidade() { return this.movimentacaoForm.get('quantidade'); }
  get dataHora() { return this.movimentacaoForm.get('dataHora'); }
  get observacoes() { return this.movimentacaoForm.get('observacoes'); }
} 