package com.dicio.dicio_android.output.graphical;

import androidx.annotation.NonNull;

/**
 * An interface that has to be implemented by classes that wish to
 * display the output generated by components.<br>
 * Some more methods could be added in the future, for example
 * `clearScreen()`, `addDivider()` or something along those lines.
 */
public interface GraphicalOutputDevice {
    void display(@NonNull OutputContainerView graphicalOutput);
}