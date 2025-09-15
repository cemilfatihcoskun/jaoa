"use strict";
Object.defineProperty(exports, Symbol.toStringTag, { value: "Module" });
const vue = require("./vue-DWle4Cai.cjs");
const superdoc = require("./index-BXEyK17K.cjs");
function self(vars) {
  const {
    opacityDisabled,
    heightTiny,
    heightSmall,
    heightMedium,
    heightLarge,
    heightHuge,
    primaryColor,
    fontSize
  } = vars;
  return {
    fontSize,
    textColor: primaryColor,
    sizeTiny: heightTiny,
    sizeSmall: heightSmall,
    sizeMedium: heightMedium,
    sizeLarge: heightLarge,
    sizeHuge: heightHuge,
    color: primaryColor,
    opacitySpinning: opacityDisabled
  };
}
const spinLight = {
  common: superdoc.derived,
  self
};
const style = superdoc.c([superdoc.c("@keyframes spin-rotate", `
 from {
 transform: rotate(0);
 }
 to {
 transform: rotate(360deg);
 }
 `), superdoc.cB("spin-container", `
 position: relative;
 `, [superdoc.cB("spin-body", `
 position: absolute;
 top: 50%;
 left: 50%;
 transform: translateX(-50%) translateY(-50%);
 `, [superdoc.fadeInTransition()])]), superdoc.cB("spin-body", `
 display: inline-flex;
 align-items: center;
 justify-content: center;
 flex-direction: column;
 `), superdoc.cB("spin", `
 display: inline-flex;
 height: var(--n-size);
 width: var(--n-size);
 font-size: var(--n-size);
 color: var(--n-color);
 `, [superdoc.cM("rotate", `
 animation: spin-rotate 2s linear infinite;
 `)]), superdoc.cB("spin-description", `
 display: inline-block;
 font-size: var(--n-font-size);
 color: var(--n-text-color);
 transition: color .3s var(--n-bezier);
 margin-top: 8px;
 `), superdoc.cB("spin-content", `
 opacity: 1;
 transition: opacity .3s var(--n-bezier);
 pointer-events: all;
 `, [superdoc.cM("spinning", `
 user-select: none;
 -webkit-user-select: none;
 pointer-events: none;
 opacity: var(--n-opacity-spinning);
 `)])]);
const STROKE_WIDTH = {
  small: 20,
  medium: 18,
  large: 16
};
const spinProps = Object.assign(Object.assign({}, superdoc.useTheme.props), {
  contentClass: String,
  contentStyle: [Object, String],
  description: String,
  stroke: String,
  size: {
    type: [String, Number],
    default: "medium"
  },
  show: {
    type: Boolean,
    default: true
  },
  strokeWidth: Number,
  rotate: {
    type: Boolean,
    default: true
  },
  spinning: {
    type: Boolean,
    validator: () => {
      return true;
    },
    default: void 0
  },
  delay: Number
});
const NSpin = vue.defineComponent({
  name: "Spin",
  props: spinProps,
  slots: Object,
  setup(props) {
    if (vue.process$1.env.NODE_ENV !== "production") {
      vue.watchEffect(() => {
        if (props.spinning !== void 0) {
          superdoc.warnOnce("spin", "`spinning` is deprecated, please use `show` instead.");
        }
      });
    }
    const {
      mergedClsPrefixRef,
      inlineThemeDisabled
    } = superdoc.useConfig(props);
    const themeRef = superdoc.useTheme("Spin", "-spin", style, spinLight, props, mergedClsPrefixRef);
    const cssVarsRef = vue.computed(() => {
      const {
        size: spinSize
      } = props;
      const {
        common: {
          cubicBezierEaseInOut
        },
        self: self2
      } = themeRef.value;
      const {
        opacitySpinning,
        color,
        textColor
      } = self2;
      const size = typeof spinSize === "number" ? superdoc.pxfy(spinSize) : self2[superdoc.createKey("size", spinSize)];
      return {
        "--n-bezier": cubicBezierEaseInOut,
        "--n-opacity-spinning": opacitySpinning,
        "--n-size": size,
        "--n-color": color,
        "--n-text-color": textColor
      };
    });
    const themeClassHandle = inlineThemeDisabled ? superdoc.useThemeClass("spin", vue.computed(() => {
      const {
        size
      } = props;
      return typeof size === "number" ? String(size) : size[0];
    }), cssVarsRef, props) : void 0;
    const compitableShow = superdoc.useCompitable(props, ["spinning", "show"]);
    const activeRef = vue.ref(false);
    vue.watchEffect((onCleanup) => {
      let timerId;
      if (compitableShow.value) {
        const {
          delay
        } = props;
        if (delay) {
          timerId = window.setTimeout(() => {
            activeRef.value = true;
          }, delay);
          onCleanup(() => {
            clearTimeout(timerId);
          });
          return;
        }
      }
      activeRef.value = compitableShow.value;
    });
    return {
      mergedClsPrefix: mergedClsPrefixRef,
      active: activeRef,
      mergedStrokeWidth: vue.computed(() => {
        const {
          strokeWidth
        } = props;
        if (strokeWidth !== void 0) return strokeWidth;
        const {
          size
        } = props;
        return STROKE_WIDTH[typeof size === "number" ? "medium" : size];
      }),
      cssVars: inlineThemeDisabled ? void 0 : cssVarsRef,
      themeClass: themeClassHandle === null || themeClassHandle === void 0 ? void 0 : themeClassHandle.themeClass,
      onRender: themeClassHandle === null || themeClassHandle === void 0 ? void 0 : themeClassHandle.onRender
    };
  },
  render() {
    var _a, _b;
    const {
      $slots,
      mergedClsPrefix,
      description
    } = this;
    const rotate = $slots.icon && this.rotate;
    const descriptionNode = (description || $slots.description) && vue.h("div", {
      class: `${mergedClsPrefix}-spin-description`
    }, description || ((_a = $slots.description) === null || _a === void 0 ? void 0 : _a.call($slots)));
    const icon = $slots.icon ? vue.h("div", {
      class: [`${mergedClsPrefix}-spin-body`, this.themeClass]
    }, vue.h("div", {
      class: [`${mergedClsPrefix}-spin`, rotate && `${mergedClsPrefix}-spin--rotate`],
      style: $slots.default ? "" : this.cssVars
    }, $slots.icon()), descriptionNode) : vue.h("div", {
      class: [`${mergedClsPrefix}-spin-body`, this.themeClass]
    }, vue.h(superdoc.NBaseLoading, {
      clsPrefix: mergedClsPrefix,
      style: $slots.default ? "" : this.cssVars,
      stroke: this.stroke,
      "stroke-width": this.mergedStrokeWidth,
      class: `${mergedClsPrefix}-spin`
    }), descriptionNode);
    (_b = this.onRender) === null || _b === void 0 ? void 0 : _b.call(this);
    return $slots.default ? vue.h("div", {
      class: [`${mergedClsPrefix}-spin-container`, this.themeClass],
      style: this.cssVars
    }, vue.h("div", {
      class: [`${mergedClsPrefix}-spin-content`, this.active && `${mergedClsPrefix}-spin-content--spinning`, this.contentClass],
      style: this.contentStyle
    }, $slots), vue.h(vue.Transition, {
      name: "fade-in-transition"
    }, {
      default: () => this.active ? icon : null
    })) : icon;
  }
});
const range = (start, end) => {
  const length = end - start;
  return Array.from({ length }, (_, i) => start + i);
};
class PDFAdapter {
  /**
   * @throws {Error}
   */
  constructor() {
    const proto = Object.getPrototypeOf(this);
    if (proto.constructor === PDFAdapter) {
      throw new Error("Abstract class should not be instanciated");
    }
  }
}
class PDFJSAdapter extends PDFAdapter {
  /**
   * @param {PDFJSConfig} config
   */
  constructor(config) {
    super();
    this.pdfLib = config.pdfLib;
    this.pdfViewer = config.pdfViewer;
    this.workerSrc = config.workerSrc;
    this.textLayerMode = config.textLayerMode ?? 0;
    if (config.setWorker) {
      if (this.workerSrc) {
        this.pdfLib.GlobalWorkerOptions.workerSrc = config.workerSrc;
      } else {
        this.pdfLib.GlobalWorkerOptions.workerSrc = getWorkerSrcFromCDN(this.pdfLib.version);
      }
    }
    this.pdfPageViews = [];
  }
  /**
   * @param {string | ArrayBuffer | Uint8Array} file
   * @returns {Promise<PDFDocumentProxy>}
   */
  async getDocument(file) {
    const loadingTask = this.pdfLib.getDocument(file);
    const document2 = await loadingTask.promise;
    return document2;
  }
  /**
   * @param {RenderPagesOptions} options
   * @returns {Promise<void>}
   */
  async renderPages({ documentId, pdfDocument, viewerContainer, emit = () => {
  } }) {
    try {
      this.pdfPageViews = [];
      const numPages = pdfDocument.numPages;
      const firstPage = 1;
      const pdfjsPages = await getPdfjsPages(pdfDocument, firstPage, numPages);
      const pageContainers = [];
      for (const [index, page] of pdfjsPages.entries()) {
        const container = document.createElement("div");
        container.classList.add("pdf-page");
        container.dataset.pageNumber = (index + 1).toString();
        container.id = `${documentId}-page-${index + 1}`;
        pageContainers.push(container);
        const { width, height } = this.getOriginalPageSize(page);
        const scale = 1;
        const eventBus = new this.pdfViewer.EventBus();
        const pdfPageView = new this.pdfViewer.PDFPageView({
          container,
          id: index + 1,
          scale,
          defaultViewport: page.getViewport({ scale }),
          eventBus,
          textLayerMode: this.textLayerMode
        });
        this.pdfPageViews.push(pdfPageView);
        const containerBounds = container.getBoundingClientRect();
        containerBounds.originalWidth = width;
        containerBounds.originalHeight = height;
        pdfPageView.setPdfPage(page);
        await pdfPageView.draw();
        emit("page-loaded", documentId, index, containerBounds);
      }
      viewerContainer.append(...pageContainers);
      emit("ready", documentId, viewerContainer);
    } catch (err) {
      console.error("Error loading PDF:", err);
    }
  }
  /**
   * @param {PDFPageProxy} page
   * @returns {object}
   */
  getOriginalPageSize(page) {
    const viewport = page.getViewport({ scale: 1 });
    const width = viewport.width;
    const height = viewport.height;
    return { width, height };
  }
  /**
   * @return {void}
   */
  destroy() {
    this.pdfPageViews.forEach((view) => view.destroy());
    this.pdfPageViews = [];
  }
}
class PDFAdapterFactory {
  /**
   * @param {PDFJSConfig & {adapter: AdapterType}} config
   * @returns {PDFAdapter}
   * @throws {Error}
   */
  static create(config) {
    const adapters = {
      pdfjs: () => {
        return new PDFJSAdapter(config);
      },
      default: () => {
        throw new Error("Unsupported adapter");
      }
    };
    const adapter = adapters[config.adapter] ?? adapters.default;
    return adapter();
  }
}
const createPDFConfig = (config) => {
  const defaultConfig = {
    adapter: "pdfjs"
  };
  return {
    ...defaultConfig,
    ...config
  };
};
async function getPdfjsPages(pdf, firstPage, lastPage) {
  const pagesPromises = range(firstPage, lastPage + 1).map((num) => pdf.getPage(num));
  return await Promise.all(pagesPromises);
}
function getWorkerSrcFromCDN(version) {
  return `https://cdnjs.cloudflare.com/ajax/libs/pdf.js/${version}/pdf.worker.min.mjs`;
}
const readFileAsArrayBuffer = (blob) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (event) => resolve(event.target.result);
    reader.onerror = reject;
    reader.readAsDataURL(blob);
  });
};
const _hoisted_1 = {
  key: 0,
  class: "superdoc-pdf-viewer__loader"
};
const _sfc_main = {
  __name: "PdfViewer",
  props: {
    documentData: {
      type: Object,
      required: true
    },
    config: {
      type: Object,
      required: true
    }
  },
  emits: ["page-loaded", "ready", "selection-change", "bypass-selection"],
  setup(__props, { emit: __emit }) {
    const emit = __emit;
    const props = __props;
    const superdocStore = superdoc.useSuperdocStore();
    const { activeZoom } = superdoc.storeToRefs(superdocStore);
    const viewer = vue.ref(null);
    const isReady = vue.ref(false);
    const id = props.documentData.id;
    const pdfData = props.documentData.data;
    const pdfConfig = createPDFConfig({
      pdfLib: props.config.pdfLib,
      pdfViewer: props.config.pdfViewer,
      workerSrc: props.config.workerSrc,
      setWorker: props.config.setWorker,
      textLayerMode: props.config.textLayerMode
    });
    const pdfAdapter = PDFAdapterFactory.create(pdfConfig);
    const loadPDF = async (file) => {
      try {
        const result = await readFileAsArrayBuffer(file);
        const document2 = await pdfAdapter.getDocument(result);
        await pdfAdapter.renderPages({
          documentId: id,
          pdfDocument: document2,
          viewerContainer: viewer.value,
          emit
        });
        isReady.value = true;
      } catch {
      }
    };
    function getSelectedTextBoundingBox(container) {
      const selection = window.getSelection();
      if (selection.rangeCount === 0) {
        return null;
      }
      const range2 = selection.getRangeAt(0);
      const boundingRects = range2.getClientRects();
      if (boundingRects.length === 0) {
        return null;
      }
      const firstRect = boundingRects[0];
      let boundingBox = {
        top: firstRect.top,
        left: firstRect.left,
        bottom: firstRect.bottom,
        right: firstRect.right
      };
      for (let i = 1; i < boundingRects.length; i++) {
        const rect = boundingRects[i];
        if (rect.width === 0 || rect.height === 0) {
          continue;
        }
        boundingBox.top = Math.min(boundingBox.top, rect.top);
        boundingBox.left = Math.min(boundingBox.left, rect.left);
        boundingBox.bottom = Math.max(boundingBox.bottom, rect.bottom);
        boundingBox.right = Math.max(boundingBox.right, rect.right);
      }
      const containerRect = container.getBoundingClientRect();
      viewer.value.getBoundingClientRect();
      boundingBox.top = (boundingBox.top - containerRect.top) / (activeZoom.value / 100) + container.scrollTop;
      boundingBox.left = (boundingBox.left - containerRect.left) / (activeZoom.value / 100) + container.scrollLeft;
      boundingBox.bottom = (boundingBox.bottom - containerRect.top) / (activeZoom.value / 100) + container.scrollTop;
      boundingBox.right = (boundingBox.right - containerRect.left) / (activeZoom.value / 100) + container.scrollLeft;
      return boundingBox;
    }
    const handlePdfClick = (e) => {
      const { target } = e;
      if (target.tagName !== "SPAN") {
        emit("bypass-selection", e);
      }
    };
    const handleMouseUp = (e) => {
      const selection = window.getSelection();
      if (selection.toString().length > 0) {
        const selectionBounds = getSelectedTextBoundingBox(viewer.value);
        const sel = superdoc.useSelection({
          selectionBounds,
          documentId: id
        });
        emit("selection-change", sel);
      }
    };
    vue.onMounted(async () => {
      await loadPDF(pdfData);
    });
    vue.onUnmounted(() => {
      pdfAdapter.destroy();
    });
    return (_ctx, _cache) => {
      return vue.openBlock(), vue.createElementBlock("div", {
        class: "superdoc-pdf-viewer-container",
        onMousedown: handlePdfClick,
        onMouseup: handleMouseUp
      }, [
        vue.createBaseVNode("div", {
          class: "superdoc-pdf-viewer",
          ref_key: "viewer",
          ref: viewer,
          id: "viewerId"
        }, null, 512),
        !isReady.value ? (vue.openBlock(), vue.createElementBlock("div", _hoisted_1, [
          vue.createVNode(vue.unref(NSpin), {
            class: "superdoc-pdf-viewer__spin",
            size: "large"
          })
        ])) : vue.createCommentVNode("", true)
      ], 32);
    };
  }
};
const PdfViewer = /* @__PURE__ */ superdoc._export_sfc(_sfc_main, [["__scopeId", "data-v-84719682"]]);
exports.default = PdfViewer;
