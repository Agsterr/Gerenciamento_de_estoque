import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MovimentacaoProduto, TipoMovimentacao } from '../../models/movimentacao-produto.model';

export interface CorrecaoMovimentacaoModalData {
  movimentacao: MovimentacaoProduto;
}

@Component({
  selector: 'app-correcao-movimentacao-modal',
  templateUrl: './correcao-movimentacao-modal.component.html',
  styleUrls: ['./correcao-movimentacao-modal.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
  ],
})
export class CorrecaoMovimentacaoModalComponent {
  form: FormGroup;
  loading = false;
  readonly TipoMovimentacao = TipoMovimentacao;

  constructor(
    private dialogRef: MatDialogRef<CorrecaoMovimentacaoModalComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CorrecaoMovimentacaoModalData,
    private fb: FormBuilder,
  ) {
    this.form = this.fb.group({
      quantidadeCorreta: [
        data.movimentacao.quantidade,
        [Validators.required, Validators.min(0), Validators.max(999999)],
      ],
      motivo: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(500)]],
    });
  }

  get tipoLabel(): string {
    return this.data.movimentacao.tipo === TipoMovimentacao.ENTRADA ? 'Entrada' : 'Saída';
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading = true;
    this.dialogRef.close({
      success: true,
      quantidadeCorreta: this.form.value.quantidadeCorreta,
      motivo: this.form.value.motivo.trim(),
    });
  }

  onCancel(): void {
    this.dialogRef.close({ success: false });
  }
}
