import { Component } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'Gerenciamento_de_estoque_front';

  // Declarando o formulário como uma propriedade da classe
  form = new FormGroup({
    name: new FormControl(''),
    quantity: new FormControl('')
  });
}

