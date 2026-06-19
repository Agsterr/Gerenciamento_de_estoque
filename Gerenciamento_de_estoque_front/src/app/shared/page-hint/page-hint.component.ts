import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HelpPrerequisite } from '../help/help-content.data';

@Component({
  selector: 'app-page-hint',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './page-hint.component.html',
  styleUrls: ['./page-hint.component.scss'],
})
export class PageHintComponent {
  @Input() message = '';
  @Input() prerequisites: HelpPrerequisite[] = [];
  @Input() showHelpLink = true;
}
