import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TUTORIAL_STEPS, MODULES_HELP } from '../shared/help/help-content.data';

@Component({
  selector: 'app-ajuda',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './ajuda.component.html',
  styleUrls: ['./ajuda.component.scss'],
})
export class AjudaComponent {
  tutorialSteps = TUTORIAL_STEPS;
  modulesHelp = MODULES_HELP;
  activeTab: 'tutorial' | 'modulos' | 'mapa' = 'tutorial';
  expandedModule: string | null = null;

  setTab(tab: 'tutorial' | 'modulos' | 'mapa'): void {
    this.activeTab = tab;
  }

  toggleModule(id: string): void {
    this.expandedModule = this.expandedModule === id ? null : id;
  }
}
